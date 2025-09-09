package com.concordium.wallet.data

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
            protocolLevelTokenDao.update(
                it.copy(
                    isHidden = false,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun unmarkNewlyReceived(tokenId: String) {
        protocolLevelTokenDao.unmarkNewlyReceived(tokenId)
    }

    suspend fun updateToken(updatedEntity: ProtocolLevelTokenEntity) {
        protocolLevelTokenDao.update(updatedEntity)
    }
}
