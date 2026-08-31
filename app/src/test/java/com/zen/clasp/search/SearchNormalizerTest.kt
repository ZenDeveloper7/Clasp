package com.zen.clasp.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchNormalizerTest {
    @Test
    fun tokens_normalizeUnicodeAndDiscardQuerySyntax() {
        assertEquals(
            listOf("café", "東京", "phone", "1"),
            SearchNormalizer.tokens("  Cafe\u0301 + 東京 / PHONE:1  ")
        )
        assertEquals("café 東京", SearchNormalizer.toAppSearchQuery("Café 東京"))
        assertFalse(SearchNormalizer.toAppSearchQuery("*** OR").contains("OR"))
    }
}
