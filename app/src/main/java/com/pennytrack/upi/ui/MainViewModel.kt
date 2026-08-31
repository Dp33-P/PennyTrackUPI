package com.pennytrack.upi.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pennytrack.upi.data.model.BudgetEntity
import com.pennytrack.upi.data.model.Categories
import com.pennytrack.upi.data.model.TransactionEntity
import com.pennytrack.upi.data.model.TransactionKind
import com.pennytrack.upi.data.model.TransactionType
import com.pennytrack.upi.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CategoryTotal(
    val category: String,
    val amountPaise: Long
)

data class MainUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val reviewQueue: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val todaySpendPaise: Long = 0,
    val monthSpendPaise: Long = 0,
    val yearSpendPaise: Long = 0,
    val fixedThisMonthPaise: Long = 0,
    val monthlyBudgetPaise: Long = 30_000_00,
    val safePerDayPaise: Long = 0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val insights: List<String> = emptyList(),
    val statusMessage: String? = null,
    val isImporting: Boolean = false
)

class MainViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val status = MutableStateFlow<String?>(null)
    private val importing = MutableStateFlow(false)

    val uiState = combine(
        repository.observeTransactions(),
        repository.observeReviewQueue(),
        repository.observeBudgets(),
        status,
        importing
    ) { transactions, reviewQueue, budgets, statusMessage, isImporting ->
        buildState(transactions, reviewQueue, budgets, statusMessage, isImporting)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    fun importSms(context: Context) {
        viewModelScope.launch {
            importing.value = true
            runCatching { repository.importSms(context.applicationContext) }
                .onSuccess { count -> status.value = "Imported $count transactions" }
                .onFailure { error -> status.value = error.message ?: "Import failed" }
            importing.value = false
        }
    }

    fun addCash(amountInput: String, category: String, merchant: String, note: String) {
        val amountPaise = MoneyFormat.parseInputToPaise(amountInput)
        if (amountPaise == null) {
            status.value = "Enter a valid amount"
            return
        }

        viewModelScope.launch {
            repository.addCashExpense(
                amountPaise = amountPaise,
                category = category,
                merchantName = merchant.ifBlank { null },
                note = note.ifBlank { null }
            )
            status.value = "Cash expense saved"
        }
    }

    fun updateCategory(transaction: TransactionEntity, category: String) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transaction, category)
            status.value = "Category saved"
        }
    }

    fun excludeTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.setExcluded(transactionId, true)
            status.value = "Excluded from reports"
        }
    }

    fun keepTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.markReviewed(transactionId)
            status.value = "Kept"
        }
    }

    fun setMonthlyBudget(input: String) {
        val amountPaise = MoneyFormat.parseInputToPaise(input)
        if (amountPaise == null) {
            status.value = "Enter a valid budget"
            return
        }
        viewModelScope.launch {
            repository.setMonthlyBudget(amountPaise)
            status.value = "Budget saved"
        }
    }

    fun clearStatus() {
        status.value = null
    }

    private fun buildState(
        transactions: List<TransactionEntity>,
        reviewQueue: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        statusMessage: String?,
        isImporting: Boolean
    ): MainUiState {
        val now = LocalDate.now()
        val todayStart = now.startMillis()
        val tomorrowStart = now.plusDays(1).startMillis()
        val month = YearMonth.from(now)
        val monthStart = month.atDay(1).startMillis()
        val nextMonthStart = month.plusMonths(1).atDay(1).startMillis()
        val yearStart = now.withDayOfYear(1).startMillis()
        val nextYearStart = now.plusYears(1).withDayOfYear(1).startMillis()

        val spendTransactions = transactions.filter { it.isSpend() }
        val todaySpend = spendTransactions.sumBetween(todayStart, tomorrowStart)
        val monthSpend = spendTransactions.sumBetween(monthStart, nextMonthStart)
        val yearSpend = spendTransactions.sumBetween(yearStart, nextYearStart)
        val fixedMonth = spendTransactions
            .filter { it.kind == TransactionKind.FIXED_BILL || it.kind == TransactionKind.EMI_LOAN }
            .sumBetween(monthStart, nextMonthStart)

        val categoryTotals = spendTransactions
            .filter { it.dateMillis in monthStart until nextMonthStart }
            .groupBy { it.category }
            .map { (category, entries) -> CategoryTotal(category, entries.sumOf { it.amountPaise }) }
            .sortedByDescending { it.amountPaise }

        val monthlyBudget = budgets.firstOrNull { it.category == "Overall" }?.monthlyLimitPaise ?: 30_000_00
        val remainingDays = (month.lengthOfMonth() - now.dayOfMonth + 1).coerceAtLeast(1)
        val safePerDay = ((monthlyBudget - monthSpend).coerceAtLeast(0) / remainingDays)

        return MainUiState(
            transactions = transactions,
            reviewQueue = reviewQueue,
            budgets = budgets,
            todaySpendPaise = todaySpend,
            monthSpendPaise = monthSpend,
            yearSpendPaise = yearSpend,
            fixedThisMonthPaise = fixedMonth,
            monthlyBudgetPaise = monthlyBudget,
            safePerDayPaise = safePerDay,
            categoryTotals = categoryTotals,
            insights = buildInsights(spendTransactions, monthStart, nextMonthStart, categoryTotals),
            statusMessage = statusMessage,
            isImporting = isImporting
        )
    }

    private fun buildInsights(
        spendTransactions: List<TransactionEntity>,
        monthStart: Long,
        nextMonthStart: Long,
        categoryTotals: List<CategoryTotal>
    ): List<String> {
        val monthTransactions = spendTransactions.filter { it.dateMillis in monthStart until nextMonthStart }
        val top = categoryTotals.firstOrNull()
        val smallSpends = monthTransactions.filter { it.amountPaise <= 100_00 }
        val insights = mutableListOf<String>()

        top?.let {
            insights += "Top category: ${it.category} at ${MoneyFormat.format(it.amountPaise)}"
        }
        if (smallSpends.size >= 3) {
            insights += "${smallSpends.size} small payments total ${MoneyFormat.format(smallSpends.sumOf { it.amountPaise })}"
        }
        val uncategorized = monthTransactions.count { it.category == Categories.OTHER }
        if (uncategorized > 0) {
            insights += "$uncategorized transactions need review"
        }
        return insights
    }

    private fun TransactionEntity.isSpend(): Boolean {
        return type == TransactionType.DEBIT &&
            kind != TransactionKind.TRANSFER &&
            kind != TransactionKind.REFUND &&
            kind != TransactionKind.INCOME
    }

    private fun List<TransactionEntity>.sumBetween(startMillis: Long, endMillis: Long): Long {
        return filter { it.dateMillis in startMillis until endMillis }.sumOf { it.amountPaise }
    }

    private fun LocalDate.startMillis(): Long {
        return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

class MainViewModelFactory(
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}
