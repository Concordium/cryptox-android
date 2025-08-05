package com.concordium.wallet.data

import com.concordium.wallet.data.room.ContractTokenEntity
import com.concordium.wallet.data.room.ContractTokenDao

class ContractTokensRepository(private val contractTokenDao: ContractTokenDao) {
    suspend fun insert(contractToken: ContractTokenEntity) {
        contractTokenDao.insert(contractToken)
    }

    suspend fun getTokens(accountAddress: String, contractIndex: String): List<ContractTokenEntity> {
        return contractTokenDao.getTokens(accountAddress, contractIndex)
    }

    suspend fun getTokens(
        accountAddress: String,
        isFungible: Boolean? = null,
    ): List<ContractTokenEntity> {
        return if (isFungible != null)
            contractTokenDao.getTokens(accountAddress, isFungible)
        else
            contractTokenDao.getTokens(accountAddress)
    }

    suspend fun find(
        accountAddress: String,
        contractIndex: String,
        token: String
    ): ContractTokenEntity? {
        return contractTokenDao.find(accountAddress, contractIndex, token)
    }

    suspend fun delete(accountAddress: String, contractIndex: String, token: String) {
        contractTokenDao.delete(accountAddress, contractIndex, token)
    }

    suspend fun unmarkNewlyReceived(tokenUid: String) {
        contractTokenDao.unmarkNewlyReceived(tokenUid)
    }
}
