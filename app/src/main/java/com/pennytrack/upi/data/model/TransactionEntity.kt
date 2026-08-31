package com.pennytrack.upi.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("dateMillis"),
        Index("merchantKey"),
        Index("upiRef"),
        Index("amountPaise", "type")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountPaise: Long,
    val currency: String = "INR",
    val type: TransactionType,
    val kind: TransactionKind,
    val category: String,
    val merchantName: String?,
    val merchantKey: String?,
    val accountHint: String?,
    val upiRef: String?,
    val source: TransactionSource,
    val rawText: String?,
    val dateMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val duplicateOfId: Long? = null,
    val duplicateConfidence: Int = 0,
    val duplicateReasons: String? = null,
    val excludeFromReports: Boolean = false,
    val isReviewed: Boolean = true,
    val note: String? = null
)
