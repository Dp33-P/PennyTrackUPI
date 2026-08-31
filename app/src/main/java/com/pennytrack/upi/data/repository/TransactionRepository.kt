package com.pennytrack.upi.data.repository

import android.content.Context
import com.pennytrack.upi.data.dao.BudgetDao
import com.pennytrack.upi.data.dao.MerchantDao
import com.pennytrack.upi.data.dao.TransactionDao
import com.pennytrack.upi.data.model.BudgetEntity
import com.pennytrack.upi.data.model.Categories
import com.pennytrack.upi.data.model.MerchantEntity
import com.pennytrack.upi.data.model.TransactionEntity
import com.pennytrack.upi.data.model.TransactionKind
import com.pennytrack.upi.data.model.TransactionSource
import com.pennytrack.upi.data.model.TransactionType
import com.pennytrack.upi.engine.CategoryEngine
import com.pennytrack.upi.engine.DuplicateDetector
import com.pennytrack.upi.engine.SeedRules
import com.pennytrack.upi.engine.TextNormalizer
import com.pennytrack.upi.engine.TransactionParser
import com.pennytrack.upi.sms.SmsImportManager
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantDao: MerchantDao,
    private val budgetDao: BudgetDao
) {
    fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeReportable()

    fun observeReviewQueue(): Flow<List<TransactionEntity>> = transactionDao.observeReviewQueue()

    fun observeMerchants(): Flow<List<MerchantEntity>> = merchantDao.observeAll()

    fun observeBudgets(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    suspend fun seedDefaults() {
        budgetDao.upsert(BudgetEntity(category = "Overall", monthlyLimitPaise = 30_000_00))
        SeedRules.knownMerchants.forEach { known ->
            merchantDao.insertIgnore(
                MerchantEntity(
                    key = known.key,
                    displayName = known.displayName,
                    category = known.category,
                    kind = known.kind,
                    aliases = known.alias,
                    source = "built_in"
                )
            )
        }
    }

    suspend fun importSms(context: Context, maxMessages: Int = 2_500): Int {
        var imported = 0
        SmsImportManager.readInbox(context, maxMessages).forEach { sms ->
            val inserted = captureText(
                text = sms.body,
                source = TransactionSource.SMS,
                dateMillis = sms.dateMillis
            )
            if (inserted != null) imported += 1
        }
        return imported
    }

    suspend fun captureText(
        text: String,
        source: TransactionSource,
        dateMillis: Long = System.currentTimeMillis()
    ): Long? {
        val parsed = TransactionParser.parse(text, source, dateMillis) ?: return null
        val merchant = resolveMerchant(parsed.merchantName, parsed.merchantKey, parsed.rawText, parsed.type)
        val categoryGuess = merchant?.let {
            com.pennytrack.upi.engine.CategoryGuess(it.category, it.kind, 92)
        } ?: CategoryEngine.guess(parsed.merchantName, parsed.rawText, parsed.type)

        val baseTransaction = TransactionEntity(
            amountPaise = parsed.amountPaise,
            type = parsed.type,
            kind = categoryGuess.kind,
            category = categoryGuess.category,
            merchantName = merchant?.displayName ?: parsed.merchantName,
            merchantKey = merchant?.key ?: parsed.merchantKey,
            accountHint = parsed.accountHint,
            upiRef = parsed.upiRef,
            source = source,
            rawText = parsed.rawText,
            dateMillis = parsed.dateMillis,
            isReviewed = categoryGuess.category != Categories.OTHER
        )

        val duplicate = findDuplicate(baseTransaction)
        val transaction = if (duplicate != null) {
            baseTransaction.copy(
                duplicateOfId = duplicate.transactionId,
                duplicateConfidence = duplicate.score,
                duplicateReasons = duplicate.reasons.joinToString(", "),
                excludeFromReports = duplicate.shouldAutoExclude,
                isReviewed = duplicate.shouldAutoExclude
            )
        } else {
            baseTransaction
        }

        val id = transactionDao.insert(transaction)
        transaction.merchantKey?.let { merchantKey ->
            merchantDao.touch(merchantKey)
        }
        return id
    }

    suspend fun addCashExpense(
        amountPaise: Long,
        category: String,
        merchantName: String?,
        note: String?,
        dateMillis: Long = System.currentTimeMillis()
    ): Long {
        val merchantKey = TextNormalizer.key(merchantName)
        val kind = CategoryEngine.kindForCategory(category).let {
            if (category == Categories.CASH) TransactionKind.CASH_SPEND else it
        }

        if (!merchantName.isNullOrBlank() && merchantKey != null) {
            val existing = merchantDao.getByKey(merchantKey)
            merchantDao.upsert(
                existing?.copy(
                    displayName = TextNormalizer.displayName(merchantName),
                    category = category,
                    kind = kind,
                    usageCount = existing.usageCount + 1,
                    lastSeenMillis = dateMillis,
                    userPinned = true
                ) ?: MerchantEntity(
                    key = merchantKey,
                    displayName = TextNormalizer.displayName(merchantName),
                    category = category,
                    kind = kind,
                    source = "cash",
                    usageCount = 1,
                    lastSeenMillis = dateMillis,
                    userPinned = true
                )
            )
        }

        return transactionDao.insert(
            TransactionEntity(
                amountPaise = amountPaise,
                type = TransactionType.DEBIT,
                kind = kind,
                category = category,
                merchantName = merchantName?.let(TextNormalizer::displayName),
                merchantKey = merchantKey,
                accountHint = null,
                upiRef = null,
                source = TransactionSource.CASH,
                rawText = note,
                dateMillis = dateMillis,
                isReviewed = true,
                note = note
            )
        )
    }

    suspend fun updateTransactionCategory(transaction: TransactionEntity, category: String) {
        val kind = CategoryEngine.kindForCategory(category, transaction.type)
        transactionDao.updateCategory(transaction.id, category, kind)
        transaction.merchantKey?.let { key ->
            val merchant = merchantDao.getByKey(key)
            if (merchant != null) {
                merchantDao.upsert(
                    merchant.copy(
                        category = category,
                        kind = kind,
                        userPinned = true,
                        lastSeenMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun setExcluded(transactionId: Long, excluded: Boolean) {
        transactionDao.setExcluded(transactionId, excluded)
    }

    suspend fun markReviewed(transactionId: Long) {
        transactionDao.markReviewed(transactionId)
    }

    suspend fun setMonthlyBudget(amountPaise: Long) {
        budgetDao.upsert(BudgetEntity(category = "Overall", monthlyLimitPaise = amountPaise))
    }

    private suspend fun resolveMerchant(
        merchantName: String?,
        merchantKey: String?,
        rawText: String,
        type: TransactionType
    ): MerchantEntity? {
        if (merchantKey != null) {
            merchantDao.getByKey(merchantKey)?.let { return it }
        }

        val known = SeedRules.knownMerchants.firstOrNull { known ->
            val haystack = "${merchantName.orEmpty()} $rawText".lowercase()
            haystack.contains(known.alias.lowercase())
        }

        val guessed = CategoryEngine.guess(merchantName, rawText, type)
        val key = merchantKey ?: TextNormalizer.key(merchantName) ?: return null
        val displayName = merchantName?.let(TextNormalizer::displayName) ?: known?.displayName ?: return null

        val merchant = MerchantEntity(
            key = key,
            displayName = displayName,
            category = known?.category ?: guessed.category,
            kind = known?.kind ?: guessed.kind,
            aliases = listOfNotNull(merchantName, known?.alias).distinct().joinToString(","),
            source = if (known != null) "built_in_match" else "learned",
            usageCount = 0
        )
        merchantDao.insertIgnore(merchant)
        return merchantDao.getByKey(key) ?: merchant
    }

    private suspend fun findDuplicate(transaction: TransactionEntity): com.pennytrack.upi.engine.DuplicateResult? {
        val windowStart = transaction.dateMillis - 12 * 60 * 60 * 1_000
        val windowEnd = transaction.dateMillis + 12 * 60 * 60 * 1_000
        val candidates = transactionDao.findPotentialDuplicates(
            amountPaise = transaction.amountPaise,
            type = transaction.type,
            startMillis = windowStart,
            endMillis = windowEnd
        )
        return DuplicateDetector.bestMatch(transaction, candidates)
    }
}
