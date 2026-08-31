package com.pennytrack.upi.engine

import com.pennytrack.upi.data.model.TransactionSource
import com.pennytrack.upi.data.model.TransactionType
import java.util.Locale

object TransactionParser {
    private val debitKeywords = listOf(
        "debited", "paid", "sent", "spent", "withdrawn", "purchase", "payment made",
        "transferred from", "has been deducted", "deducted"
    )
    private val creditKeywords = listOf(
        "credited", "received", "deposited", "refund", "reversal", "cashback", "has been added"
    )
    private val ignoreKeywords = listOf(
        "otp", "one time password", "verification code", "valid for", "do not share",
        "mandate request", "collect request", "autopay request"
    )
    private val upiRefPatterns = listOf(
        Regex("""(?:upi\s*(?:ref(?:erence)?|txn|transaction)?\s*(?:no|id)?|utr|rrn|ref(?:erence)?\s*(?:no|id)?|txn\s*id)[:\s#.-]*([A-Z0-9]{6,30})""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:UPI|UTR)([0-9]{8,20})\b""", RegexOption.IGNORE_CASE)
    )
    private val accountPatterns = listOf(
        Regex("""(?:a/c|acct|account)\s*(?:no\.?)?\s*([xX*]*\d{3,6})""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:XX|xx|X{2,}|\*{2,})(\d{3,6})\b""")
    )
    private val vpaPattern = Regex("""([a-zA-Z0-9._-]{2,}@[a-zA-Z0-9._-]{2,})""")
    private val merchantPatterns = listOf(
        Regex("""(?:paid\s+to|sent\s+to|transferred\s+to|to|at|towards)\s+([A-Z0-9a-z ._&@-]{2,80})""", RegexOption.IGNORE_CASE),
        Regex("""(?:merchant|payee|beneficiary|vpa|upi id)[:\s-]+([A-Z0-9a-z ._&@-]{2,80})""", RegexOption.IGNORE_CASE),
        Regex("""(?:info|narration)[:\s-]+([A-Z0-9a-z ._&@-]{2,80})""", RegexOption.IGNORE_CASE)
    )

    fun parse(rawText: String, source: TransactionSource, dateMillis: Long): ParsedTransaction? {
        val text = TextNormalizer.compact(rawText)
        val lower = text.lowercase(Locale.ENGLISH)

        if (ignoreKeywords.any { lower.contains(it) }) return null

        val amountPaise = MoneyParser.parseAmountPaise(text) ?: return null
        val type = detectType(lower) ?: return null

        val merchant = extractMerchant(text)
        val cleanMerchant = merchant?.let { TextNormalizer.displayName(it) }
        val merchantKey = TextNormalizer.key(cleanMerchant ?: merchant)

        return ParsedTransaction(
            amountPaise = amountPaise,
            type = type,
            merchantName = cleanMerchant,
            merchantKey = merchantKey,
            accountHint = extractFirst(accountPatterns, text)?.uppercase(Locale.ENGLISH),
            upiRef = extractFirst(upiRefPatterns, text)?.uppercase(Locale.ENGLISH),
            rawText = text,
            source = source,
            dateMillis = dateMillis
        )
    }

    private fun detectType(lower: String): TransactionType? {
        val debitIndex = debitKeywords.mapNotNull { keyword ->
            lower.indexOf(keyword).takeIf { it >= 0 }
        }.minOrNull()
        val creditIndex = creditKeywords.mapNotNull { keyword ->
            lower.indexOf(keyword).takeIf { it >= 0 }
        }.minOrNull()

        return when {
            debitIndex == null && creditIndex == null -> null
            debitIndex != null && creditIndex == null -> TransactionType.DEBIT
            debitIndex == null && creditIndex != null -> TransactionType.CREDIT
            debitIndex != null && creditIndex != null && debitIndex < creditIndex -> TransactionType.DEBIT
            else -> TransactionType.CREDIT
        }
    }

    private fun extractMerchant(text: String): String? {
        vpaPattern.find(text)?.groupValues?.getOrNull(1)?.let { return it }

        merchantPatterns.forEach { pattern ->
            val candidate = pattern.find(text)?.groupValues?.getOrNull(1)
                ?.let(::trimMerchantCandidate)
            if (!candidate.isNullOrBlank()) return candidate
        }

        return null
    }

    private fun trimMerchantCandidate(value: String): String {
        val stopWords = listOf(
            " upi ref", " utr", " rrn", " ref ", " on ", " from ", " via ",
            " avl", " available", " balance", " bal ", " if ", " txn "
        )
        var result = value.trim()
        val lower = result.lowercase(Locale.ENGLISH)
        val stopIndex = stopWords.mapNotNull { stop ->
            lower.indexOf(stop).takeIf { it >= 0 }
        }.minOrNull()
        if (stopIndex != null) result = result.substring(0, stopIndex)

        return result.trim(' ', '.', ',', '-', ':')
    }

    private fun extractFirst(patterns: List<Regex>, text: String): String? {
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)
        }?.trim()
    }
}
