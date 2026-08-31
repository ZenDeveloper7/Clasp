package com.zen.clasp.search

import com.zen.clasp.model.Capture
import com.zen.clasp.model.CaptureType
import com.zen.clasp.model.DeletionState
import com.zen.clasp.model.ExtractionState
import com.zen.clasp.model.ProcessingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class SearchLatencyTest {
    @Test
    fun rank_largeSyntheticCandidateSet_staysWithinHostBaseline() {
        val captures = (0 until LIBRARY_SIZE).map { index -> capture(index) }
        val hits = captures.map { capture ->
            IndexedSearchHit(
                captureId = capture.id,
                matchedField = SearchField.ORIGINAL,
                excerpt = capture.originalText.orEmpty().take(140),
                relevanceScore = if (capture.id == "4999") 10.0 else 1.0
            )
        }
        lateinit var results: List<SearchResult>

        val elapsed = measureTimeMillis {
            results = SearchRanker.rank(
                hits = hits,
                captures = captures,
                filters = SearchFilters(),
                nowMillis = NOW
            )
        }

        assertEquals("4999", results.first().capture.id)
        assertTrue("Ranking took ${elapsed}ms", elapsed < HOST_BUDGET_MS)
        println("Clasp search ranking baseline: $LIBRARY_SIZE candidates in ${elapsed}ms")
    }

    private fun capture(index: Int) = Capture(
        id = index.toString(),
        type = CaptureType.TEXT,
        createdAt = index.toLong(),
        updatedAt = index.toLong(),
        sourcePackage = null,
        originalText = if (index == LIBRARY_SIZE - 1) {
            "unique needle is intentionally near the end"
        } else {
            "ordinary capture number $index"
        },
        userTitle = null,
        userNote = null,
        isFavorite = false,
        processingState = ProcessingState.STORED,
        extractedText = null,
        extractionState = ExtractionState.NOT_APPLICABLE,
        extractionErrorCode = null,
        contentRevision = 1,
        deletionState = DeletionState.ACTIVE,
        errorCode = null,
        attachments = emptyList()
    )

    private companion object {
        const val LIBRARY_SIZE = 5_000
        const val HOST_BUDGET_MS = 2_000L
        const val NOW = 2_000_000_000_000L
    }
}
