package com.zen.clasp.search

import com.zen.clasp.model.Capture
import com.zen.clasp.model.CaptureType
import com.zen.clasp.model.ExtractionState
import java.util.concurrent.TimeUnit

object SearchRanker {
    fun topResults(results: List<SearchResult>, limit: Int): List<SearchResult> = results
        .sortedWith(resultComparator)
        .take(limit)

    fun rank(
        hits: List<IndexedSearchHit>,
        captures: List<Capture>,
        filters: SearchFilters,
        nowMillis: Long = System.currentTimeMillis()
    ): List<SearchResult> {
        val captureById = captures.associateBy(Capture::id)
        return hits.mapNotNull { hit ->
            val capture = captureById[hit.captureId] ?: return@mapNotNull null
            if (!capture.matches(filters, nowMillis)) return@mapNotNull null
            val favouriteBoost = if (capture.isFavorite) 2.0 else 0.0
            val ageDays = TimeUnit.MILLISECONDS.toDays((nowMillis - capture.createdAt).coerceAtLeast(0))
            val recencyBoost = (2.0 - (ageDays / 30.0)).coerceIn(0.0, 2.0)

            SearchResult(
                capture = capture,
                matchedField = hit.matchedField,
                excerpt = hit.excerpt,
                score = hit.relevanceScore + favouriteBoost + recencyBoost
            )
        }.sortedWith(resultComparator)
    }

    private fun Capture.matches(filters: SearchFilters, nowMillis: Long): Boolean {
        if (filters.type != null && type != filters.type) return false
        if (filters.favouriteOnly && !isFavorite) return false
        filters.dateRange.days?.let { days ->
            val cutoff = nowMillis - TimeUnit.DAYS.toMillis(days.toLong())
            if (createdAt < cutoff) return false
        }
        return when (filters.extraction) {
            ExtractionFilter.ANY -> true
            ExtractionFilter.OCR_READY -> type == CaptureType.IMAGE &&
                extractionState in setOf(ExtractionState.COMPLETE, ExtractionState.EMPTY)
            ExtractionFilter.NEEDS_ATTENTION -> type == CaptureType.IMAGE &&
                extractionState == ExtractionState.FAILED
        }
    }

    private val resultComparator = compareByDescending<SearchResult> { it.score }
        .thenByDescending { it.capture.createdAt }
}
