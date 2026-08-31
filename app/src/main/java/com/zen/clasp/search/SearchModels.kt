package com.zen.clasp.search

import com.zen.clasp.model.Capture
import com.zen.clasp.model.CaptureType

data class SearchFilters(
    val type: CaptureType? = null,
    val favouriteOnly: Boolean = false,
    val dateRange: SearchDateRange = SearchDateRange.ANY_TIME,
    val extraction: ExtractionFilter = ExtractionFilter.ANY
)

enum class SearchDateRange(val displayName: String, val days: Int?) {
    ANY_TIME("Any time", null),
    LAST_7_DAYS("7 days", 7),
    LAST_30_DAYS("30 days", 30)
}

enum class ExtractionFilter(val displayName: String) {
    ANY("Any state"),
    OCR_READY("OCR ready"),
    NEEDS_ATTENTION("Needs attention")
}

enum class SearchField(val displayName: String) {
    TITLE("Title"),
    NOTE("Note"),
    ORIGINAL("Original"),
    OCR("OCR"),
    ATTACHMENT("File name")
}

data class SearchResult(
    val capture: Capture,
    val matchedField: SearchField,
    val excerpt: String,
    val score: Double
)
