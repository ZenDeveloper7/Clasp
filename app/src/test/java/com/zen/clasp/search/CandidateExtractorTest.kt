package com.zen.clasp.search

import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateExtractorTest {
    @Test
    fun extract_findsDeterministicCandidatesWithoutDuplicates() {
        val candidates = CandidateExtractor.extract(
            "Email hello@example.com or hello@example.com. Visit https://example.com on 2026-09-01. " +
                "Call +91 98765 43210."
        )

        assertEquals(
            listOf(CandidateType.EMAIL, CandidateType.LINK, CandidateType.PHONE, CandidateType.DATE),
            candidates.map { it.type }
        )
        assertEquals(1, candidates.count { it.value == "hello@example.com" })
    }
}
