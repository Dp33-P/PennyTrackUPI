package com.pennytrack.upi.engine

import com.pennytrack.upi.data.model.Categories
import com.pennytrack.upi.data.model.TransactionKind
import com.pennytrack.upi.data.model.TransactionType
import java.util.Locale

data class CategoryGuess(
    val category: String,
    val kind: TransactionKind,
    val confidence: Int
)

object CategoryEngine {
    fun guess(
        merchantName: String?,
        rawText: String,
        transactionType: TransactionType
    ): CategoryGuess {
        val haystack = "${merchantName.orEmpty()} $rawText".lowercase(Locale.ENGLISH)

        if (transactionType == TransactionType.CREDIT) {
            return when {
                containsAny(haystack, "refund", "reversal", "cashback", "returned") ->
                    CategoryGuess(Categories.REFUND, TransactionKind.REFUND, 95)
                else -> CategoryGuess(Categories.INCOME, TransactionKind.INCOME, 70)
            }
        }

        if (containsAny(haystack, "self transfer", "own account", "to self", "between your accounts")) {
            return CategoryGuess(Categories.TRANSFER, TransactionKind.TRANSFER, 88)
        }

        SeedRules.categoryRules.forEach { rule ->
            if (rule.keywords.any { keyword -> haystack.contains(keyword.lowercase(Locale.ENGLISH)) }) {
                return CategoryGuess(rule.category, rule.kind, 86)
            }
        }

        return CategoryGuess(Categories.OTHER, TransactionKind.DAILY_SPEND, 35)
    }

    fun kindForCategory(category: String, type: TransactionType = TransactionType.DEBIT): TransactionKind {
        if (type == TransactionType.CREDIT) {
            return if (category == Categories.REFUND) TransactionKind.REFUND else TransactionKind.INCOME
        }

        return SeedRules.categoryRules
            .firstOrNull { it.category == category }
            ?.kind
            ?: when (category) {
                Categories.CASH -> TransactionKind.CASH_SPEND
                Categories.TRANSFER -> TransactionKind.TRANSFER
                Categories.REFUND -> TransactionKind.REFUND
                Categories.INCOME -> TransactionKind.INCOME
                else -> TransactionKind.DAILY_SPEND
            }
    }

    private fun containsAny(value: String, vararg needles: String): Boolean {
        return needles.any { value.contains(it) }
    }
}
