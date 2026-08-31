package com.zen.clasp.search

import java.text.Normalizer
import java.util.Locale

object SearchNormalizer {
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(separators, " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    fun tokens(value: String): List<String> = normalize(value)
        .split(' ')
        .filter(String::isNotBlank)
        .distinct()
        .take(MAX_QUERY_TOKENS)

    fun toAppSearchQuery(value: String): String = tokens(value).joinToString(" ")

    private const val MAX_QUERY_TOKENS = 12
}
