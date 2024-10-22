package com.concordium.wallet.core.multiwallet

import com.concordium.wallet.App
import com.concordium.wallet.core.AppCore

class AddAndActivateWalletUseCase() {
    /**
     * Adds and activates an extra wallet of the given [walletType].
     * Once the wallet is added, a new session is started with it.
     * On completion, the main screen must be re-started.
     *
     * @return ID if the added wallet
     *
     * @see AppCore.startNewSession
     */
    suspend operator fun invoke(
        walletType: AppWallet.Type,
    ): String {
        val newWallet = AppWallet.extra(
            type = walletType,
        )

        App.appCore.walletRepository.addWallet(newWallet)
        App.appCore.startNewSession(
            activeWallet = newWallet,
            isLoggedIn = true,
        )
        return newWallet.id
    }
}
