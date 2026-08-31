package com.pennytrack.upi.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pennytrack.upi.data.model.MerchantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchants ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): MerchantEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(merchant: MerchantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(merchant: MerchantEntity)

    @Update
    suspend fun update(merchant: MerchantEntity)

    @Query(
        """
        UPDATE merchants
        SET usageCount = usageCount + 1, lastSeenMillis = :lastSeenMillis
        WHERE key = :key
        """
    )
    suspend fun touch(key: String, lastSeenMillis: Long = System.currentTimeMillis())
}
