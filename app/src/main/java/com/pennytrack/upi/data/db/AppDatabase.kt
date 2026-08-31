package com.pennytrack.upi.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pennytrack.upi.data.dao.BudgetDao
import com.pennytrack.upi.data.dao.MerchantDao
import com.pennytrack.upi.data.dao.TransactionDao
import com.pennytrack.upi.data.model.BudgetEntity
import com.pennytrack.upi.data.model.MerchantEntity
import com.pennytrack.upi.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        MerchantEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantDao(): MerchantDao
    abstract fun budgetDao(): BudgetDao
}
