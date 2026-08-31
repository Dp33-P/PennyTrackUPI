package com.pennytrack.upi.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pennytrack.upi.data.model.TransactionEntity
import com.pennytrack.upi.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE excludeFromReports = 0 ORDER BY dateMillis DESC")
    fun observeReportable(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE excludeFromReports = 0
        AND dateMillis BETWEEN :startMillis AND :endMillis
        ORDER BY dateMillis DESC
        """
    )
    fun observeRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (isReviewed = 0 OR duplicateConfidence BETWEEN 55 AND 77)
        ORDER BY dateMillis DESC
        """
    )
    fun observeReviewQueue(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE amountPaise = :amountPaise
        AND type = :type
        AND dateMillis BETWEEN :startMillis AND :endMillis
        ORDER BY dateMillis DESC
        LIMIT 30
        """
    )
    suspend fun findPotentialDuplicates(
        amountPaise: Long,
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET category = :category, kind = :kind, isReviewed = 1 WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String, kind: com.pennytrack.upi.data.model.TransactionKind)

    @Query("UPDATE transactions SET excludeFromReports = :excluded, isReviewed = 1 WHERE id = :id")
    suspend fun setExcluded(id: Long, excluded: Boolean)

    @Query("UPDATE transactions SET isReviewed = 1 WHERE id = :id")
    suspend fun markReviewed(id: Long)
}
