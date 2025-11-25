package com.concordium.wallet.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentRecipientDao {

    @Query("SELECT * FROM recent_recipient_table ORDER BY added_at DESC LIMIT 3")
    fun getAllAsFlow(): Flow<List<RecentRecipientEntity>>

    @Query("SELECT * FROM recent_recipient_table WHERE address = :address")
    suspend fun getByAddress(address: String): RecentRecipientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recentRecipient: RecentRecipientEntity)

    @Transaction
    suspend fun update(recentRecipient: RecentRecipientEntity) {
        val existingRecentRecipient = getByAddress(recentRecipient.address)
        if (existingRecentRecipient != null) {
            delete(existingRecentRecipient)
            insert(recentRecipient)
        }
    }

    @Delete
    suspend fun delete(recentRecipient: RecentRecipientEntity)
}