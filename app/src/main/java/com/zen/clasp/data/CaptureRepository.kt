package com.zen.clasp.data

import android.content.ContentResolver
import android.net.Uri
import com.zen.clasp.model.Attachment
import com.zen.clasp.model.Capture
import com.zen.clasp.model.CaptureType
import com.zen.clasp.model.DeletionState
import com.zen.clasp.model.ExtractionState
import com.zen.clasp.model.ProcessingState
import com.zen.clasp.processing.OcrEngine
import com.zen.clasp.processing.OcrScheduler
import com.zen.clasp.search.CaptureSearchIndex
import com.zen.clasp.search.SearchFilters
import com.zen.clasp.search.SearchRanker
import com.zen.clasp.search.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

data class ExportSpec(
    val mimeType: String,
    val suggestedFileName: String
)

interface CaptureRepository {
    val captures: Flow<List<Capture>>

    suspend fun createText(text: String, sourcePackage: String? = null): String
    suspend fun importUri(uri: Uri, declaredMimeType: String?, sourcePackage: String? = null): String
    suspend fun update(captureId: String, title: String?, note: String?)
    suspend fun setFavorite(captureId: String, favorite: Boolean)
    suspend fun delete(captureId: String)
    suspend fun exportSpec(captureId: String): ExportSpec
    suspend fun export(captureId: String, destination: Uri)
    suspend fun search(query: String, filters: SearchFilters): List<SearchResult>
    suspend fun retryOcr(captureId: String)
}

enum class OcrRunResult {
    SUCCESS,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
}

class DefaultCaptureRepository(
    private val captureDao: CaptureDao,
    private val attachmentStore: FileAttachmentStore,
    private val contentResolver: ContentResolver,
    private val ocrEngine: OcrEngine,
    private val ocrScheduler: OcrScheduler,
    private val searchIndex: CaptureSearchIndex,
    private val now: () -> Long = System::currentTimeMillis
) : CaptureRepository {
    private val searchIndexMutex = Mutex()

    @Volatile
    private var searchIndexNeedsRebuild = true

    override val captures: Flow<List<Capture>> = captureDao.observeAll().map { captures ->
        captures.map { capture -> capture.toModel() }
    }

    override suspend fun createText(text: String, sourcePackage: String?): String {
        val content = text.trim()
        require(content.isNotEmpty()) { "Text cannot be empty" }
        val id = UUID.randomUUID().toString()
        val timestamp = now()
        captureDao.insert(
            capture = CaptureEntity(
                id = id,
                type = CaptureType.TEXT.name,
                createdAt = timestamp,
                updatedAt = timestamp,
                sourcePackage = sourcePackage,
                originalText = content,
                userTitle = null,
                userNote = null,
                isFavorite = false,
                processingState = ProcessingState.STORED.name,
                extractedText = null,
                extractionState = ExtractionState.NOT_APPLICABLE.name,
                extractionErrorCode = null,
                contentRevision = 1,
                deletionState = DeletionState.ACTIVE.name,
                errorCode = null
            ),
            attachment = null
        )
        syncCaptureToSearch(id)
        return id
    }

    override suspend fun importUri(
        uri: Uri,
        declaredMimeType: String?,
        sourcePackage: String?
    ): String {
        val id = UUID.randomUUID().toString()
        val stored = attachmentStore.copyFrom(contentResolver, uri, id, declaredMimeType)
        val timestamp = now()
        try {
            captureDao.insert(
                capture = CaptureEntity(
                    id = id,
                    type = if (stored.mimeType.startsWith("image/")) {
                        CaptureType.IMAGE.name
                    } else {
                        CaptureType.FILE.name
                    },
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    sourcePackage = sourcePackage,
                    originalText = null,
                    userTitle = null,
                    userNote = null,
                    isFavorite = false,
                    processingState = ProcessingState.STORED.name,
                    extractedText = null,
                    extractionState = if (stored.mimeType.startsWith("image/")) {
                        ExtractionState.PENDING.name
                    } else {
                        ExtractionState.NOT_APPLICABLE.name
                    },
                    extractionErrorCode = null,
                    contentRevision = 1,
                    deletionState = DeletionState.ACTIVE.name,
                    errorCode = null
                ),
                attachment = AttachmentEntity(
                    id = stored.id,
                    captureId = id,
                    relativePath = stored.relativePath,
                    originalDisplayName = stored.originalDisplayName,
                    mimeType = stored.mimeType,
                    sizeBytes = stored.sizeBytes,
                    sha256 = stored.sha256
                )
            )
        } catch (error: Throwable) {
            runCatching { attachmentStore.deleteCapture(id) }
            throw error
        }
        if (stored.mimeType.startsWith("image/")) {
            try {
                ocrScheduler.schedule(id)
            } catch (_: Exception) {
                captureDao.updateExtractionState(
                    id,
                    ExtractionState.FAILED.name,
                    OCR_SCHEDULING_FAILED,
                    now()
                )
            }
        }
        syncCaptureToSearch(id)
        return id
    }

    override suspend fun update(captureId: String, title: String?, note: String?) {
        captureDao.updateEditableFields(
            captureId = captureId,
            title = title?.trim()?.takeIf(String::isNotEmpty),
            note = note?.trim()?.takeIf(String::isNotEmpty),
            updatedAt = now()
        )
        syncCaptureToSearch(captureId)
    }

    override suspend fun setFavorite(captureId: String, favorite: Boolean) {
        captureDao.updateFavorite(captureId, favorite, now())
    }

    override suspend fun delete(captureId: String) {
        ocrScheduler.cancel(captureId)
        captureDao.updateDeletionState(
            captureId,
            DeletionState.DELETING.name,
            errorCode = null,
            updatedAt = now()
        )
        try {
            attachmentStore.deleteCapture(captureId)
            captureDao.deleteById(captureId)
            removeCaptureFromSearch(captureId)
        } catch (error: Throwable) {
            captureDao.updateDeletionState(
                captureId,
                DeletionState.FAILED.name,
                errorCode = DELETE_FAILED,
                updatedAt = now()
            )
            throw error
        }
    }

    override suspend fun exportSpec(captureId: String): ExportSpec {
        val capture = captureDao.getById(captureId)?.toModel()
            ?: throw IllegalArgumentException("Capture does not exist")
        val attachment = capture.attachments.firstOrNull()
        return if (attachment != null) {
            ExportSpec(
                mimeType = attachment.mimeType,
                suggestedFileName = attachmentStore.suggestedFileName(
                    attachment.originalDisplayName,
                    attachment.mimeType
                )
            )
        } else {
            ExportSpec("text/plain", "clasp-${capture.createdAt}.txt")
        }
    }

    override suspend fun export(captureId: String, destination: Uri) {
        val capture = captureDao.getById(captureId)?.toModel()
            ?: throw IllegalArgumentException("Capture does not exist")
        val attachment = capture.attachments.firstOrNull()
        if (attachment != null) {
            attachmentStore.exportTo(contentResolver, attachment.relativePath, destination)
            return
        }

        withContext(Dispatchers.IO) {
            val content = buildString {
                append(capture.userTitle?.let { "$it\n\n" }.orEmpty())
                append(capture.originalText.orEmpty())
                append(capture.userNote?.let { "\n\nNote\n$it" }.orEmpty())
            }
            contentResolver.openOutputStream(destination, "wt")?.bufferedWriter()?.use { writer ->
                writer.write(content)
            } ?: throw IOException("Unable to open export destination")
        }
    }

    override suspend fun search(query: String, filters: SearchFilters): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        ensureSearchIndex()
        val hits = searchIndex.search(query)
        if (hits.isEmpty()) return emptyList()
        val searchTime = now()
        val matchingCaptures = hits.map { it.captureId }
            .distinct()
            .chunked(DATABASE_ID_BATCH_SIZE)
            .flatMap { ids -> captureDao.getByIds(ids) }
            .map { capture -> capture.toModel() }
        return withContext(Dispatchers.Default) {
            SearchRanker.rank(
                hits = hits,
                captures = matchingCaptures,
                filters = filters,
                nowMillis = searchTime
            ).take(SEARCH_RESULT_LIMIT)
        }
    }

    override suspend fun retryOcr(captureId: String) {
        val capture = captureDao.getById(captureId)?.toModel()
            ?: throw IllegalArgumentException("Capture does not exist")
        require(capture.type == CaptureType.IMAGE) { "Only image captures can use OCR" }
        captureDao.updateExtractionState(
            captureId,
            ExtractionState.PENDING.name,
            errorCode = null,
            updatedAt = now()
        )
        ocrScheduler.schedule(captureId, replace = true)
    }

    suspend fun runOcr(captureId: String): OcrRunResult {
        val capture = captureDao.getById(captureId)?.toModel()
            ?: return OcrRunResult.PERMANENT_FAILURE
        if (capture.type != CaptureType.IMAGE || capture.deletionState != DeletionState.ACTIVE) {
            return OcrRunResult.PERMANENT_FAILURE
        }
        val attachment = capture.attachments.firstOrNull()
            ?: return failOcr(captureId, OCR_ATTACHMENT_MISSING, retryable = false)

        captureDao.updateExtractionState(
            captureId,
            ExtractionState.RUNNING.name,
            errorCode = null,
            updatedAt = now()
        )
        return try {
            val text = ocrEngine.recognize(attachmentStore.fileFor(attachment.relativePath))
                .trim()
                .take(MAX_EXTRACTED_TEXT_CHARS)
            captureDao.updateExtractionResult(
                captureId = captureId,
                text = text.takeIf(String::isNotEmpty),
                state = if (text.isEmpty()) ExtractionState.EMPTY.name else ExtractionState.COMPLETE.name,
                updatedAt = now()
            )
            syncCaptureToSearch(captureId)
            OcrRunResult.SUCCESS
        } catch (error: CancellationException) {
            captureDao.updateExtractionState(
                captureId,
                ExtractionState.PENDING.name,
                errorCode = null,
                updatedAt = now()
            )
            throw error
        } catch (_: IOException) {
            failOcr(captureId, OCR_IO_FAILED, retryable = true)
        } catch (_: Exception) {
            failOcr(captureId, OCR_RECOGNITION_FAILED, retryable = false)
        }
    }

    suspend fun enqueuePendingOcr() {
        captureDao.getPendingExtractionIds().forEach { captureId ->
            runCatching { ocrScheduler.schedule(captureId) }
        }
    }

    suspend fun resumeInterruptedDeletions() {
        captureDao.getInterruptedDeletions().forEach { capture ->
            runCatching { delete(capture.capture.id) }
        }
    }

    suspend fun synchronizeSearchIndex() {
        searchIndexMutex.withLock {
            val captures = captureDao.getAll().map { capture -> capture.toModel() }
            searchIndex.synchronize(captures)
            searchIndexNeedsRebuild = false
        }
    }

    private suspend fun ensureSearchIndex() {
        if (searchIndexNeedsRebuild) synchronizeSearchIndex()
    }

    private suspend fun syncCaptureToSearch(captureId: String) {
        try {
            searchIndexMutex.withLock {
                captureDao.getById(captureId)?.toModel()?.let { searchIndex.upsert(it) }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            searchIndexNeedsRebuild = true
        }
    }

    private suspend fun removeCaptureFromSearch(captureId: String) {
        try {
            searchIndexMutex.withLock { searchIndex.remove(captureId) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            searchIndexNeedsRebuild = true
        }
    }

    private fun CaptureWithAttachments.toModel() = Capture(
        id = capture.id,
        type = CaptureType.fromStorage(capture.type),
        createdAt = capture.createdAt,
        updatedAt = capture.updatedAt,
        sourcePackage = capture.sourcePackage,
        originalText = capture.originalText,
        userTitle = capture.userTitle,
        userNote = capture.userNote,
        isFavorite = capture.isFavorite,
        processingState = ProcessingState.fromStorage(capture.processingState),
        extractedText = capture.extractedText,
        extractionState = ExtractionState.fromStorage(capture.extractionState),
        extractionErrorCode = capture.extractionErrorCode,
        contentRevision = capture.contentRevision,
        deletionState = DeletionState.fromStorage(capture.deletionState),
        errorCode = capture.errorCode,
        attachments = attachments.map { attachment ->
            Attachment(
                id = attachment.id,
                relativePath = attachment.relativePath,
                originalDisplayName = attachment.originalDisplayName,
                mimeType = attachment.mimeType,
                sizeBytes = attachment.sizeBytes,
                sha256 = attachment.sha256
            )
        }
    )

    private suspend fun failOcr(
        captureId: String,
        errorCode: String,
        retryable: Boolean
    ): OcrRunResult {
        captureDao.updateExtractionState(
            captureId,
            ExtractionState.FAILED.name,
            errorCode,
            now()
        )
        return if (retryable) OcrRunResult.RETRYABLE_FAILURE else OcrRunResult.PERMANENT_FAILURE
    }

    companion object {
        private const val DELETE_FAILED = "DELETE_FAILED"
        private const val OCR_ATTACHMENT_MISSING = "OCR_ATTACHMENT_MISSING"
        private const val OCR_IO_FAILED = "OCR_IO_FAILED"
        private const val OCR_RECOGNITION_FAILED = "OCR_RECOGNITION_FAILED"
        private const val OCR_SCHEDULING_FAILED = "OCR_SCHEDULING_FAILED"
        private const val DATABASE_ID_BATCH_SIZE = 500
        private const val SEARCH_RESULT_LIMIT = 250
        private const val MAX_EXTRACTED_TEXT_CHARS = 250_000
    }
}
