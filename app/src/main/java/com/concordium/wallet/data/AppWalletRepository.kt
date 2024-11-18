package com.concordium.wallet.data

import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.data.room.app.AppWalletDao
import com.concordium.wallet.data.room.app.AppWalletEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AppWalletRepository(
    private val appWalletDao: AppWalletDao,
) {
    suspend fun getActiveWallet(): AppWallet =
        appWalletDao.getActive()
            .let(::AppWallet)

    fun getWalletsFlow(): Flow<List<AppWallet>> =
        appWalletDao.getAll()
            .map { rows ->
                rows.map(::AppWallet)
            }

    suspend fun getWallets(): List<AppWallet> =
        getWalletsFlow().first()

    suspend fun addWallet(wallet: AppWallet) {
        appWalletDao.insertAndActivate(AppWalletEntity(wallet))
    }

    suspend fun switchWalletType(
        walletId: String,
        newType: AppWallet.Type
    ) {
        appWalletDao.switchType(
            walletId = walletId,
            newType = newType.name,
        )
    }

    suspend fun activate(
        newActiveWallet: AppWallet,
    ) {
        appWalletDao.activate(
            walletId = newActiveWallet.id,
        )
    }

    suspend fun delete(
        walletToDeleteId: String,
        walletToActivateId: String,
    ) {
        appWalletDao.deleteAndActivateAnother(
            walletToDeleteId = walletToDeleteId,
            walletToActivateId = walletToActivateId,
        )
    }
}
