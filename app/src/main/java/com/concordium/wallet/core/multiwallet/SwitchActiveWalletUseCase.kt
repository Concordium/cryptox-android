package com.concordium.wallet.core.multiwallet

import com.concordium.wallet.App
import com.concordium.wallet.core.AppCore

class SwitchActiveWalletUseCase {
    /**
     * Switches the active wallet to the given [newActiveWallet].
     * Once the wallet is activated, a new session is started with it.
     * On completion, the main screen must be re-started.
     *
     * @see AppCore.startNewSession
     */
    suspend operator fun invoke(
        newActiveWallet: AppWallet,
    ) {
        App.appCore.walletRepository.activate(newActiveWallet)
        App.appCore.startNewSession(
            activeWallet = newActiveWallet,
        )
    }
}
