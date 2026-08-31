package com.pennytrack.upi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey
    val key: String,
    val displayName: String,
    val category: String,
    val kind: TransactionKind = TransactionKind.DAILY_SPEND,
    val aliases: String = "",
    val vpa: String? = null,
    val source: String = "learned",
    val usageCount: Int = 0,
    val lastSeenMillis: Long = System.currentTimeMillis(),
    val userPinned: Boolean = false
)
