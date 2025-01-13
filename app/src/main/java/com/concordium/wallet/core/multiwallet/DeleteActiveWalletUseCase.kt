package com.concordium.wallet.core.multiwallet

import com.concordium.wallet.App

class DeleteActiveWalletUseCase {
    /**
     * Deletes the current active wallet and switches it to the [newActiveWallet].
     * Once the [newActiveWallet] is activated, a new session is started with it.
     * On completion, the main screen must be re-started.
     *
     * @see SwitchActiveWalletUseCase
     */
    suspend operator fun invoke(
        newActiveWallet: AppWallet,
    ) {
        check(App.appCore.walletRepository.getWallets().size > 1) {
            "Can't delete the only wallet"
        }

        App.appCore.walletRepository.delete(
            walletToDeleteId = App.appCore.session.activeWallet.id,
            walletToActivateId = newActiveWallet.id,
        )
        App.appCore.session.walletStorage.erase()

        SwitchActiveWalletUseCase().invoke(
            newActiveWallet = newActiveWallet,
        )
    }
}
