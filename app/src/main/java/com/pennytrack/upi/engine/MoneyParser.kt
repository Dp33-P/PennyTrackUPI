package com.pennytrack.upi.engine

import kotlin.math.roundToLong

object MoneyParser {
    private val amountPatterns = listOf(
        Regex("""(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:rs\.?|inr)""", RegexOption.IGNORE_CASE)
    )

    fun parseAmountPaise(text: String): Long? {
        val match = amountPatterns.firstNotNullOfOrNull { it.find(text) } ?: return null
        val rupees = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (rupees <= 0.0) return null
        return (rupees * 100.0).roundToLong()
    }
}
