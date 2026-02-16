package com.concordium.wallet.data.room.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppNetworkDao {
    @Query("SELECT * FROM networks ORDER BY created_at ASC")
    abstract fun getAll(): Flow<List<AppNetworkEntity>>

    @Query("SELECT COUNT(*) FROM networks")
    abstract suspend fun getCount(): Int

    @Query("SELECT * FROM networks WHERE is_active=1")
    abstract suspend fun getActive(): AppNetworkEntity

    @Query("UPDATE networks SET is_active = CASE WHEN genesis_hash=:genesisHash THEN 1 ELSE 0 END")
    abstract suspend fun activate(genesisHash: String)

    @Insert
    abstract suspend fun insert(vararg networks: AppNetworkEntity)

    @Transaction
    open suspend fun insertAndActivate(network: AppNetworkEntity) {
        insert(network)
        activate(network.genesisHash)
    }
}
