package com.concordium.wallet.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ContractTokenDao {
    @Query("SELECT * FROM contract_token_table WHERE account_address = :accountAddress")
    suspend fun getTokens(accountAddress: String): List<ContractTokenEntity>

    @Query("SELECT * FROM contract_token_table WHERE is_fungible = :isFungible AND account_address = :accountAddress")
    suspend fun getTokens(accountAddress: String, isFungible: Boolean): List<ContractTokenEntity>

    @Query("SELECT * FROM contract_token_table WHERE contract_index = :contractIndex AND account_address = :accountAddress")
    suspend fun getTokens(accountAddress: String, contractIndex: String): List<ContractTokenEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg contractToken: ContractTokenEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg contractToken: ContractTokenEntity)

    @Query("DELETE FROM contract_token_table WHERE contract_index = :contractIndex AND token_id = :token AND account_address = :accountAddress")
    suspend fun delete(accountAddress: String, contractIndex: String, token: String)

    @Query("SELECT * FROM contract_token_table WHERE contract_index = :contractIndex AND token_id = :token AND account_address = :accountAddress")
    suspend fun find(accountAddress: String, contractIndex: String, token: String): ContractTokenEntity?

    @Query("UPDATE contract_token_table SET is_newly_received = 0 WHERE token_id = :tokenUid")
    suspend fun unmarkNewlyReceived(tokenUid: String)
}
