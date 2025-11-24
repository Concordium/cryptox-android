package com.concordium.wallet.data

import com.concordium.wallet.data.room.RecentRecipientDao
import com.concordium.wallet.data.room.RecentRecipientEntity

class RecentRecipientRepository(private val recentRecipientDao: RecentRecipientDao) {

    val allRecentRecipients = recentRecipientDao.getAllAsFlow()

    suspend fun getByAddress(address: String) =
        recentRecipientDao.getByAddress(address)


    suspend fun insert(recentRecipient: RecentRecipientEntity) {
        recentRecipientDao.insert(recentRecipient)
    }

    suspend fun update(recentRecipient: RecentRecipientEntity) {
        recentRecipientDao.update(recentRecipient)
    }

    suspend fun delete(recentRecipient: RecentRecipientEntity) {
        recentRecipientDao.delete(recentRecipient)
    }

}