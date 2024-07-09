package com.concordium.wallet.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ContractTokenDao {
    @Query("SELECT * FROM contract_token_table WHERE account_address = :accountAddress")
    suspend fun getTokens(accountAddress: String): List<ContractToken>

    @Query("SELECT * FROM contract_token_table WHERE is_fungible = :isFungible AND account_address = :accountAddress")
    suspend fun getTokens(accountAddress: String, isFungible: Boolean): List<ContractToken>

    @Query("SELECT * FROM contract_token_table WHERE contract_index = :contractIndex AND account_address = :accountAddress")
    suspend fun getTokens(accountAddress: String, contractIndex: String): List<ContractToken>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg contractToken: ContractToken)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg contractToken: ContractToken)

    @Query("DELETE FROM contract_token_table WHERE contract_index = :contractIndex AND token_id = :tokenId AND account_address = :accountAddress")
    suspend fun delete(accountAddress: String, contractIndex: String, tokenId: String)

    @Query("SELECT * FROM contract_token_table WHERE contract_index = :contractIndex AND token_id = :tokenId AND account_address = :accountAddress")
    suspend fun find(accountAddress: String, contractIndex: String, tokenId: String): ContractToken?
}
