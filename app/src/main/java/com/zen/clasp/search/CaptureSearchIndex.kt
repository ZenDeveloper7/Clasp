package com.zen.clasp.search

import android.content.Context
import androidx.appsearch.app.AppSearchSchema
import androidx.appsearch.app.AppSearchResult
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.RemoveByDocumentIdRequest
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.localstorage.LocalStorage
import com.zen.clasp.model.Capture
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class IndexedSearchHit(
    val captureId: String,
    val matchedField: SearchField,
    val excerpt: String,
    val relevanceScore: Double
)

interface CaptureSearchIndex {
    suspend fun synchronize(captures: List<Capture>)
    suspend fun rebuild(captures: List<Capture>)
    suspend fun upsert(capture: Capture)
    suspend fun remove(captureId: String)
    suspend fun search(query: String): List<IndexedSearchHit>
}

class AppSearchCaptureIndex(context: Context) : CaptureSearchIndex {
    private val applicationContext = context.applicationContext
    private val sessionMutex = Mutex()

    @Volatile
    private var cachedSession: AppSearchSession? = null

    override suspend fun synchronize(captures: List<Capture>) {
        val session = session()
        val indexedRevisions = indexedRevisions(session)
        val capturesById = captures.associateBy(Capture::id)
        val staleIds = indexedRevisions.keys - capturesById.keys
        staleIds.chunked(INDEX_BATCH_SIZE).forEach { ids -> remove(session, ids) }
        captures.filter { capture -> indexedRevisions[capture.id] != capture.contentRevision.toLong() }
            .chunked(INDEX_BATCH_SIZE)
            .forEach { batch -> put(session, batch) }
        session.requestFlushAsync().await()
    }

    override suspend fun rebuild(captures: List<Capture>) {
        val session = session()
        session.removeAsync("", searchSpec(includeSnippets = false)).await()
        captures.chunked(INDEX_BATCH_SIZE).forEach { batch ->
            put(session, batch)
        }
        session.requestFlushAsync().await()
    }

    override suspend fun upsert(capture: Capture) {
        val session = session()
        put(session, listOf(capture))
        session.requestFlushAsync().await()
    }

    override suspend fun remove(captureId: String) {
        val session = session()
        remove(session, listOf(captureId))
        session.requestFlushAsync().await()
    }

    override suspend fun search(query: String): List<IndexedSearchHit> {
        val normalizedQuery = SearchNormalizer.toAppSearchQuery(query)
        if (normalizedQuery.isBlank()) return emptyList()

        val results = session().search(normalizedQuery, searchSpec(includeSnippets = true))
        return try {
            buildList {
                while (true) {
                    val page = results.nextPageAsync.await()
                    if (page.isEmpty()) break
                    page.mapTo(this) { result ->
                        val match = result.matchInfos.minByOrNull { info ->
                            FIELD_PRIORITY[info.propertyPath] ?: Int.MAX_VALUE
                        }
                        IndexedSearchHit(
                            captureId = result.genericDocument.id,
                            matchedField = match?.propertyPath.toSearchField(),
                            excerpt = match?.snippet?.toString().orEmpty(),
                            relevanceScore = result.rankingSignal
                        )
                    }
                }
            }
        } finally {
            results.close()
        }
    }

    private suspend fun session(): AppSearchSession {
        cachedSession?.let { return it }
        return sessionMutex.withLock {
            cachedSession?.let { return@withLock it }
            val context = LocalStorage.SearchContext.Builder(applicationContext, DATABASE_NAME).build()
            val session = LocalStorage.createSearchSessionAsync(context).await()
            session.setSchemaAsync(
                SetSchemaRequest.Builder()
                    .addSchemas(SCHEMA)
                    .build()
            ).await()
            cachedSession = session
            session
        }
    }

    private suspend fun put(session: AppSearchSession, captures: List<Capture>) {
        if (captures.isEmpty()) return
        val request = PutDocumentsRequest.Builder()
            .addGenericDocuments(captures.map { capture -> capture.toGenericDocument() })
            .build()
        check(session.putAsync(request).await().isSuccess) {
            "AppSearch failed to index one or more captures"
        }
    }

    private suspend fun remove(session: AppSearchSession, captureIds: List<String>) {
        if (captureIds.isEmpty()) return
        val request = RemoveByDocumentIdRequest.Builder(NAMESPACE)
            .addIds(captureIds)
            .build()
        val result = session.removeAsync(request).await()
        if (!result.isSuccess && result.failures.values.any {
                it.resultCode != AppSearchResult.RESULT_NOT_FOUND
            }
        ) {
            error("AppSearch failed to remove one or more captures")
        }
    }

    private suspend fun indexedRevisions(session: AppSearchSession): Map<String, Long> {
        val results = session.search("", searchSpec(includeSnippets = false))
        return try {
            buildMap {
                while (true) {
                    val page = results.nextPageAsync.await()
                    if (page.isEmpty()) break
                    page.forEach { result ->
                        put(
                            result.genericDocument.id,
                            result.genericDocument.getPropertyLong(PROPERTY_REVISION)
                        )
                    }
                }
            }
        } finally {
            results.close()
        }
    }

    private fun searchSpec(includeSnippets: Boolean): SearchSpec {
        val builder = SearchSpec.Builder()
            .addFilterSchemas(SCHEMA_TYPE)
            .addFilterNamespaces(NAMESPACE)
            .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
            .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
            .setResultCountPerPage(SEARCH_PAGE_SIZE)
            .setPropertyWeights(SCHEMA_TYPE, PROPERTY_WEIGHTS)
        if (includeSnippets) {
            builder
                .setSnippetCount(MAX_SNIPPET_DOCUMENTS)
                .setSnippetCountPerProperty(1)
                .setMaxSnippetSize(MAX_SNIPPET_SIZE)
        }
        return builder.build()
    }

    private fun Capture.toGenericDocument(): GenericDocument {
        val builder = CaptureDocumentBuilder(NAMESPACE, id, SCHEMA_TYPE)
            .setCreationTimestampMillis(createdAt)
        builder.setNonBlankProperty(PROPERTY_TITLE, userTitle)
        builder.setNonBlankProperty(PROPERTY_NOTE, userNote)
        builder.setNonBlankProperty(PROPERTY_ORIGINAL, originalText)
        builder.setNonBlankProperty(PROPERTY_OCR, extractedText)
        builder.setNonBlankProperty(PROPERTY_ATTACHMENT, attachments.firstOrNull()?.originalDisplayName)
        builder.setPropertyLong(PROPERTY_REVISION, contentRevision.toLong())
        return builder.build()
    }

    private fun CaptureDocumentBuilder.setNonBlankProperty(name: String, value: String?) {
        value?.takeIf(String::isNotBlank)?.let { setPropertyString(name, it) }
    }

    private fun String?.toSearchField(): SearchField = when (this) {
        PROPERTY_TITLE -> SearchField.TITLE
        PROPERTY_NOTE -> SearchField.NOTE
        PROPERTY_OCR -> SearchField.OCR
        PROPERTY_ATTACHMENT -> SearchField.ATTACHMENT
        else -> SearchField.ORIGINAL
    }

    private companion object {
        const val DATABASE_NAME = "clasp_search"
        const val NAMESPACE = "captures"
        const val SCHEMA_TYPE = "ClaspCapture"
        const val PROPERTY_TITLE = "title"
        const val PROPERTY_NOTE = "note"
        const val PROPERTY_ORIGINAL = "originalText"
        const val PROPERTY_OCR = "extractedText"
        const val PROPERTY_ATTACHMENT = "attachmentName"
        const val PROPERTY_REVISION = "contentRevision"
        const val INDEX_BATCH_SIZE = 100
        const val SEARCH_PAGE_SIZE = 100
        const val MAX_SNIPPET_DOCUMENTS = 10_000
        const val MAX_SNIPPET_SIZE = 140

        val PROPERTY_WEIGHTS = mapOf(
            PROPERTY_TITLE to 12.0,
            PROPERTY_NOTE to 9.0,
            PROPERTY_ORIGINAL to 7.0,
            PROPERTY_OCR to 6.0,
            PROPERTY_ATTACHMENT to 4.0
        )

        val FIELD_PRIORITY = PROPERTY_WEIGHTS.keys.withIndex().associate { (index, property) ->
            property to index
        }

        val SCHEMA = AppSearchSchema.Builder(SCHEMA_TYPE)
            .addProperty(indexedStringProperty(PROPERTY_TITLE))
            .addProperty(indexedStringProperty(PROPERTY_NOTE))
            .addProperty(indexedStringProperty(PROPERTY_ORIGINAL))
            .addProperty(indexedStringProperty(PROPERTY_OCR))
            .addProperty(indexedStringProperty(PROPERTY_ATTACHMENT))
            .addProperty(
                AppSearchSchema.LongPropertyConfig.Builder(PROPERTY_REVISION)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                    .build()
            )
            .build()

        fun indexedStringProperty(name: String) = AppSearchSchema.StringPropertyConfig.Builder(name)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .build()
    }

    private class CaptureDocumentBuilder(namespace: String, id: String, schemaType: String) :
        GenericDocument.Builder<CaptureDocumentBuilder>(namespace, id, schemaType)
}
