package com.concordium.wallet.data

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
}