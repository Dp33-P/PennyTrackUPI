package com.pennytrack.upi.engine

import java.util.Locale

object TextNormalizer {
    fun compact(value: String): String {
        return value
            .replace('\n', ' ')
            .replace('\t', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun key(value: String?): String? {
        val cleaned = value
            ?.lowercase(Locale.ENGLISH)
            ?.replace(Regex("@[a-z0-9._-]+"), "")
            ?.replace(Regex("[^a-z0-9]+"), "")
            ?.trim()
        return cleaned?.takeIf { it.length >= 2 }
    }

    fun displayName(value: String): String {
        val cleaned = value
            .replace(Regex("@[A-Za-z0-9._-]+"), "")
            .replace(Regex("[^A-Za-z0-9& ._-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '_', '.')

        return cleaned
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                if (token.length <= 3 && token.all { it.isUpperCase() || it.isDigit() }) {
                    token
                } else {
                    token.lowercase(Locale.ENGLISH).replaceFirstChar { it.titlecase(Locale.ENGLISH) }
                }
            }
            .ifBlank { "Unknown Merchant" }
    }

    fun tokens(value: String?): Set<String> {
        return value
            ?.lowercase(Locale.ENGLISH)
            ?.split(Regex("[^a-z0-9]+"))
            ?.filter { it.length >= 3 }
            ?.toSet()
            .orEmpty()
    }
}
