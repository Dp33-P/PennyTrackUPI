package com.pennytrack.upi.engine

import com.pennytrack.upi.data.model.TransactionSource
import com.pennytrack.upi.data.model.TransactionType

data class ParsedTransaction(
    val amountPaise: Long,
    val type: TransactionType,
    val merchantName: String?,
    val merchantKey: String?,
    val accountHint: String?,
    val upiRef: String?,
    val rawText: String,
    val source: TransactionSource,
    val dateMillis: Long
)
