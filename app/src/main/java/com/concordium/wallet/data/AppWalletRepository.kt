package com.concordium.wallet.data

import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.data.room.app.AppWalletDao
import com.concordium.wallet.data.room.app.AppWalletEntity

class AppWalletRepository(
    private val appWalletDao: AppWalletDao,
) {
    suspend fun getActiveWallet(): AppWallet =
        appWalletDao
            .also { ensurePrimaryWallet() }
            .getActive()
            .let(::AppWallet)

    suspend fun getWallets(): List<AppWallet> =
        appWalletDao
            .also { ensurePrimaryWallet() }
            .getAll()
            .map(::AppWallet)

    private suspend fun ensurePrimaryWallet() {
        val primaryWallet = AppWallet.primary(
            type = AppWallet.Type.SEED,
        )
        appWalletDao.insertAndActivate(AppWalletEntity(primaryWallet))
    }
}
