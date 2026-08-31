package com.pennytrack.upi

import android.app.Application
import androidx.room.Room
import com.pennytrack.upi.data.db.AppDatabase
import com.pennytrack.upi.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PennyTrackApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "pennytrack.db"
        ).build()
    }

    val repository: TransactionRepository by lazy {
        TransactionRepository(
            transactionDao = database.transactionDao(),
            merchantDao = database.merchantDao(),
            budgetDao = database.budgetDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.seedDefaults()
        }
    }
}
