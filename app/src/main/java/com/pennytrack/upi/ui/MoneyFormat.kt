package com.pennytrack.upi.ui

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object MoneyFormat {
    private val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    fun format(paise: Long): String {
        return formatter.format(paise / 100.0)
    }

    fun parseInputToPaise(input: String): Long? {
        val cleaned = input.replace(",", "").replace("₹", "").trim()
        val rupees = cleaned.toDoubleOrNull() ?: return null
        if (rupees <= 0.0) return null
        return (rupees * 100.0).roundToLong()
    }

    fun signed(paise: Long): String {
        val prefix = if (paise < 0) "-" else ""
        return prefix + format(abs(paise))
    }
}
