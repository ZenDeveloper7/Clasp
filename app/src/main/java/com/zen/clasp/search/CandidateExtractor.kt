package com.zen.clasp.search

data class ExtractionCandidate(
    val type: CandidateType,
    val value: String
)

enum class CandidateType(val displayName: String) {
    LINK("Link"),
    EMAIL("Email"),
    PHONE("Phone"),
    DATE("Date")
}

object CandidateExtractor {
    private val patterns = listOf(
        CandidateType.EMAIL to Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE),
        CandidateType.LINK to Regex("(?:https?://|www\\.)[^\\s<>()]+", RegexOption.IGNORE_CASE),
        CandidateType.PHONE to Regex("(?<!\\w)(?:\\+?\\d[\\d ()-]{7,}\\d)"),
        CandidateType.DATE to Regex("\\b(?:\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b")
    )

    fun extract(text: String): List<ExtractionCandidate> = sequence {
        patterns.forEach { (type, pattern) ->
            pattern.findAll(text).forEach { match ->
                val candidate = ExtractionCandidate(
                    type,
                    match.value.trim().trimEnd('.', ',', ';', ':')
                )
                if (candidate.type != CandidateType.PHONE || candidate.value.count(Char::isDigit) >= 10) {
                    yield(candidate)
                }
            }
        }
    }
        .distinctBy { candidate -> candidate.type to candidate.value.lowercase() }
        .take(MAX_CANDIDATES)
        .toList()

    private const val MAX_CANDIDATES = 20
}
