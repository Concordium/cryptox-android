package com.concordium.wallet.data

import com.concordium.wallet.data.room.RecentRecipientDao
import com.concordium.wallet.data.room.RecentRecipientEntity

class RecentRecipientRepository(private val recentRecipientDao: RecentRecipientDao) {

    val allRecentRecipients = recentRecipientDao.getAllAsFlow()

    suspend fun insertOrUpdate(recentRecipient: RecentRecipientEntity) {
        recentRecipientDao.insertOrUpdate(recentRecipient)
    }
}
