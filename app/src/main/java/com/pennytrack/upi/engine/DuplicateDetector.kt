package com.pennytrack.upi.engine

import com.pennytrack.upi.data.model.TransactionEntity
import kotlin.math.abs

data class DuplicateResult(
    val transactionId: Long,
    val score: Int,
    val reasons: List<String>
) {
    val shouldAutoExclude: Boolean = score >= 78
    val shouldReview: Boolean = score in 55..77
}

object DuplicateDetector {
    fun bestMatch(newTransaction: TransactionEntity, candidates: List<TransactionEntity>): DuplicateResult? {
        return candidates
            .filter { it.id != newTransaction.id }
            .mapNotNull { candidate -> score(newTransaction, candidate) }
            .maxByOrNull { it.score }
            ?.takeIf { it.score >= 55 }
    }

    fun score(newTransaction: TransactionEntity, existing: TransactionEntity): DuplicateResult? {
        var score = 0
        val reasons = mutableListOf<String>()

        if (newTransaction.amountPaise == existing.amountPaise) {
            score += 25
            reasons += "same amount"
        }
        if (newTransaction.type == existing.type) {
            score += 10
            reasons += "same direction"
        }
        if (!newTransaction.upiRef.isNullOrBlank() && newTransaction.upiRef == existing.upiRef) {
            score += 55
            reasons += "same UPI ref"
        }
        if (!newTransaction.merchantKey.isNullOrBlank() && newTransaction.merchantKey == existing.merchantKey) {
            score += 20
            reasons += "same merchant"
        }
        if (!newTransaction.accountHint.isNullOrBlank() && newTransaction.accountHint == existing.accountHint) {
            score += 8
            reasons += "same account"
        }
        if (newTransaction.source != existing.source) {
            score += 8
            reasons += "different source"
        }

        val diffMinutes = abs(newTransaction.dateMillis - existing.dateMillis) / 60_000
        when {
            diffMinutes <= 2 -> {
                score += 20
                reasons += "within 2 minutes"
            }
            diffMinutes <= 10 -> {
                score += 12
                reasons += "within 10 minutes"
            }
            diffMinutes <= 60 -> {
                score += 6
                reasons += "within 1 hour"
            }
        }

        val similarity = textSimilarity(newTransaction.rawText, existing.rawText)
        if (similarity >= 0.45) {
            val points = (similarity * 10).toInt()
            score += points
            reasons += "similar message"
        }

        return DuplicateResult(
            transactionId = existing.id,
            score = score.coerceAtMost(100),
            reasons = reasons
        ).takeIf { it.score >= 40 }
    }

    private fun textSimilarity(left: String?, right: String?): Double {
        val leftTokens = TextNormalizer.tokens(left)
        val rightTokens = TextNormalizer.tokens(right)
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0

        val intersection = leftTokens.intersect(rightTokens).size.toDouble()
        val union = leftTokens.union(rightTokens).size.toDouble()
        return if (union == 0.0) 0.0 else intersection / union
    }
}
