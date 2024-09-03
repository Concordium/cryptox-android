package com.concordium.wallet.data

import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.data.room.ContractTokenDao

class ContractTokensRepository(private val contractTokenDao: ContractTokenDao) {
    suspend fun insert(contractToken: ContractToken) {
        contractTokenDao.insert(contractToken)
    }

    suspend fun getTokens(accountAddress: String, contractIndex: String): List<ContractToken> {
        return contractTokenDao.getTokens(accountAddress, contractIndex)
    }

    suspend fun getTokens(
        accountAddress: String,
        isFungible: Boolean? = null,
    ): List<ContractToken> {
        return if (isFungible != null)
            contractTokenDao.getTokens(accountAddress, isFungible)
        else
            contractTokenDao.getTokens(accountAddress)
    }

    suspend fun find(
        accountAddress: String,
        contractIndex: String,
        tokenId: String
    ): ContractToken? {
        return contractTokenDao.find(accountAddress, contractIndex, tokenId)
    }

    suspend fun delete(accountAddress: String, contractIndex: String, tokenId: String) {
        contractTokenDao.delete(accountAddress, contractIndex, tokenId)
    }

    suspend fun unmarkNewlyReceived(accountAddress: String, contractIndex: String, tokenId: String) {
        contractTokenDao.unmarkNewlyReceived(accountAddress, contractIndex, tokenId)
    }
}
