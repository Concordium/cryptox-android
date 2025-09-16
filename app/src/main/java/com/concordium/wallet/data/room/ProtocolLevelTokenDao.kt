package com.concordium.wallet.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.math.BigInteger

@Dao
interface ProtocolLevelTokenDao {
    @Query("SELECT * FROM protocol_level_token_table WHERE account_address = :accountAddress")
    suspend fun getTokens(accountAddress: String): List<ProtocolLevelTokenEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg protocolLevelToken: ProtocolLevelTokenEntity)

    @Update
    suspend fun update(vararg protocolLevelToken: ProtocolLevelTokenEntity)

    @Query(
        """
        UPDATE protocol_level_token_table
        SET 
        balance = :balance,
        is_in_deny_list = :isInDenyList,
        is_in_allow_list = :isInAllowList,
        is_paused = :isPaused
        WHERE token_id = :tokenId
        AND account_address = :accountAddress
    """
    )
    suspend fun update(
        tokenId: String,
        accountAddress: String,
        balance: BigInteger,
        isInDenyList: Boolean?,
        isInAllowList: Boolean?,
        isPaused: Boolean,
    )

    @Query(
        """
        SELECT * FROM protocol_level_token_table 
        WHERE LOWER(token_id) = LOWER(:tokenId) 
        AND account_address = :accountAddress
        """
    )
    suspend fun find(accountAddress: String, tokenId: String): ProtocolLevelTokenEntity?

    @Query("UPDATE protocol_level_token_table SET is_newly_received = 0 WHERE token_id = :tokenId")
    suspend fun unmarkNewlyReceived(tokenId: String)

    @Query(
        """    
        UPDATE protocol_level_token_table 
        SET is_hidden = 1 
        WHERE account_address = :accountAddress 
        AND LOWER(token_id) = LOWER(:tokenId)
    """
    )
    suspend fun hideToken(accountAddress: String, tokenId: String)
}
