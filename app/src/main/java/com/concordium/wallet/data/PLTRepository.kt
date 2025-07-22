package com.concordium.wallet.data

import com.concordium.wallet.data.model.PLTInfoWithAccountState
import com.concordium.wallet.data.model.toProtocolLevelToken
import com.concordium.wallet.data.room.ProtocolLevelToken
import com.concordium.wallet.data.room.ProtocolLevelTokenDao

class PLTRepository(private val protocolLevelTokenDao: ProtocolLevelTokenDao) {
    suspend fun insert(protocolLevelToken: ProtocolLevelToken) {
        protocolLevelTokenDao.insert(protocolLevelToken)
    }

    suspend fun getTokens(accountAddress: String): List<ProtocolLevelToken> {
        return protocolLevelTokenDao.getTokens(accountAddress)
    }

    suspend fun find(accountAddress: String, tokenId: String): ProtocolLevelToken? {
        return protocolLevelTokenDao.find(accountAddress, tokenId)
    }

    suspend fun hideToken(accountAddress: String, tokenId: String) {
        protocolLevelTokenDao.hideToken(accountAddress, tokenId)
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
                    val protocolLevelToken = tokenWithState.toProtocolLevelToken(
                        accountAddress = accountAddress,
                        addedAt = System.currentTimeMillis(),
                        isHidden = false,
                        isNewlyReceived = true
                    )
                    insert(protocolLevelToken)
                } else {
                    if (existsToken.balance != tokenWithState.tokenAccountState?.balance?.value) {
                        // Update the existing token's balance if it has changed
                        val updatedToken = tokenWithState.toProtocolLevelToken(
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