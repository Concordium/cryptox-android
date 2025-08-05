package com.concordium.wallet.data

import com.concordium.wallet.data.model.PLTInfoWithAccountState
import com.concordium.wallet.data.model.toProtocolLevelTokenEntity
import com.concordium.wallet.data.room.ProtocolLevelTokenDao
import com.concordium.wallet.data.room.ProtocolLevelTokenEntity

class PLTRepository(private val protocolLevelTokenDao: ProtocolLevelTokenDao) {
    suspend fun insert(protocolLevelToken: ProtocolLevelTokenEntity) {
        protocolLevelTokenDao.insert(protocolLevelToken)
    }

    suspend fun getTokens(accountAddress: String): List<ProtocolLevelTokenEntity> {
        return protocolLevelTokenDao.getTokens(accountAddress)
    }

    suspend fun find(accountAddress: String, tokenId: String): ProtocolLevelTokenEntity? {
        return protocolLevelTokenDao.find(accountAddress, tokenId)
    }

    suspend fun hideToken(accountAddress: String, tokenId: String) {
        protocolLevelTokenDao.hideToken(accountAddress, tokenId)
    }

    suspend fun unhideToken(accountAddress: String, tokenId: String) {
        find(accountAddress, tokenId)?.let {
            protocolLevelTokenDao.update(it.copy(isHidden = false))
        }
    }

    suspend fun unmarkNewlyReceived(tokenId: String) {
        protocolLevelTokenDao.unmarkNewlyReceived(tokenId)
    }

    suspend fun addForAccount(accountAddress: String, tokens: List<PLTInfoWithAccountState>) {
        if (tokens.isNotEmpty()) {
            tokens.forEach { tokenWithState ->
                val existsToken = find(
                    accountAddress = accountAddress,
                    tokenId = tokenWithState.token.tokenId
                )
                if (existsToken == null) {
                    val protocolLevelToken = tokenWithState.toProtocolLevelTokenEntity(
                        accountAddress = accountAddress,
                        addedAt = System.currentTimeMillis(),
                        isHidden = false,
                        isNewlyReceived = true
                    )
                    insert(protocolLevelToken)
                } else {
                    if (existsToken.balance != tokenWithState.tokenAccountState?.balance?.value) {
                        // Update the existing token's balance if it has changed
                        val updatedToken = tokenWithState.toProtocolLevelTokenEntity(
                            accountAddress = accountAddress,
                            addedAt = existsToken.addedAt,
                            isHidden = existsToken.isHidden,
                            isNewlyReceived = existsToken.isNewlyReceived
                        ).apply {
                            id = existsToken.id // Keep the same ID for the existing token
                        }
                        protocolLevelTokenDao.update(updatedToken)
                    }
                }
            }
        }
    }
}