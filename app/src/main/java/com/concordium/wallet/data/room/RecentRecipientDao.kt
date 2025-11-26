package com.concordium.wallet.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentRecipientDao {

    @Query("""
        SELECT 
        rrt.address,
        rrt.added_at,
        rt.name
    FROM recent_recipient_table AS rrt
    LEFT JOIN recipient_table AS rt ON rt.address = rrt.address
    ORDER BY rrt.added_at DESC LIMIT 3
    """)
    fun getAllAsFlow(): Flow<List<RecentRecipientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recentRecipient: RecentRecipientEntity)
}
