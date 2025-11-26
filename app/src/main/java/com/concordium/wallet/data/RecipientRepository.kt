package com.concordium.wallet.data

import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.RecipientDao

class RecipientRepository(private val recipientDao: RecipientDao) {

    val allRecipients = recipientDao.getAllAsFlow()

    suspend fun getAll(): List<Recipient> {
        return recipientDao.getAll()
    }

    suspend fun insert(recipient: Recipient) {
        recipientDao.insertUnique(recipient)
    }

    suspend fun insertAll(recipientList: List<Recipient>) {
        recipientDao.insert(*recipientList.toTypedArray())
    }

    suspend fun update(recipient: Recipient) {
        recipientDao.update(recipient)
    }

    suspend fun delete(address: String) {
        recipientDao.delete(address)
    }

    suspend fun deleteAll() {
        recipientDao.deleteAll()
    }

    suspend fun getRecipientByAddress(address: String): Recipient? {
        return recipientDao.getRecipientByAddress(address)
    }
}
