package com.pennytrack.upi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: String,
    val monthlyLimitPaise: Long,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
