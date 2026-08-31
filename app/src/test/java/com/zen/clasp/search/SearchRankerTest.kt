package com.zen.clasp.search

import com.zen.clasp.model.Capture
import com.zen.clasp.model.CaptureType
import com.zen.clasp.model.DeletionState
import com.zen.clasp.model.ExtractionState
import com.zen.clasp.model.ProcessingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankerTest {
    @Test
    fun topResults_keepsStrongestMatchesAcrossBatches() {
        val older = capture(id = "older", createdAt = NOW - 1_000)
        val newer = capture(id = "newer", createdAt = NOW)
        val results = listOf(
            SearchResult(older, SearchField.NOTE, "older", score = 5.0),
            SearchResult(newer, SearchField.TITLE, "newer", score = 10.0)
        )

        assertEquals(listOf("newer"), SearchRanker.topResults(results, limit = 1).map { it.capture.id })
    }

    @Test
    fun rank_prefersTitleMatchAndReturnsProvenance() {
        val titleCapture = capture("title", title = "Project Atlas")
        val ocrCapture = capture("ocr", extracted = "Project Atlas receipt")
        val results = SearchRanker.rank(
            hits = listOf(
                IndexedSearchHit("title", SearchField.TITLE, "Project Atlas", 12.0),
                IndexedSearchHit("ocr", SearchField.OCR, "Project Atlas receipt", 6.0)
            ),
            captures = listOf(ocrCapture, titleCapture),
            filters = SearchFilters(),
            nowMillis = NOW
        )

        assertEquals("title", results.first().capture.id)
        assertEquals(SearchField.TITLE, results.first().matchedField)
        assertEquals(SearchField.OCR, results.last().matchedField)
    }

    @Test
    fun rank_appliesTypeFavouriteDateAndOcrFilters() {
        val recentReady = capture(
            id = "ready",
            type = CaptureType.IMAGE,
            title = "Invoice",
            favourite = true,
            extractionState = ExtractionState.COMPLETE
        )
        val oldFailed = capture(
            id = "failed",
            type = CaptureType.IMAGE,
            title = "Invoice",
            createdAt = 0,
            extractionState = ExtractionState.FAILED
        )
        val results = SearchRanker.rank(
            hits = listOf(
                IndexedSearchHit("ready", SearchField.TITLE, "Invoice", 10.0),
                IndexedSearchHit("failed", SearchField.TITLE, "Invoice", 10.0)
            ),
            captures = listOf(recentReady, oldFailed),
            filters = SearchFilters(
                type = CaptureType.IMAGE,
                favouriteOnly = true,
                dateRange = SearchDateRange.LAST_7_DAYS,
                extraction = ExtractionFilter.OCR_READY
            ),
            nowMillis = NOW
        )

        assertEquals(listOf("ready"), results.map { it.capture.id })
    }

    private fun capture(
        id: String,
        type: CaptureType = CaptureType.TEXT,
        title: String? = null,
        extracted: String? = null,
        favourite: Boolean = false,
        createdAt: Long = NOW,
        extractionState: ExtractionState = ExtractionState.NOT_APPLICABLE
    ) = Capture(
        id = id,
        type = type,
        createdAt = createdAt,
        updatedAt = createdAt,
        sourcePackage = null,
        originalText = null,
        userTitle = title,
        userNote = null,
        isFavorite = favourite,
        processingState = ProcessingState.STORED,
        extractedText = extracted,
        extractionState = extractionState,
        extractionErrorCode = null,
        contentRevision = 1,
        deletionState = DeletionState.ACTIVE,
        errorCode = null,
        attachments = emptyList()
    )

    private companion object {
        const val NOW = 2_000_000_000_000L
    }
}
