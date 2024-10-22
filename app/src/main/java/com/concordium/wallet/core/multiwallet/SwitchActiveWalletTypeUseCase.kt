package com.concordium.wallet.core.multiwallet

import com.concordium.wallet.App
import com.concordium.wallet.core.AppCore
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.util.Log

class SwitchActiveWalletTypeUseCase {

    /**
     * Switches the current active wallet type to the given [newWalletType],
     * which is done when setting up the first wallet.
     * Once the wallet type is switched, a new session is started with it.
     *
     * @see AppCore.startNewSession
     */
    suspend operator fun invoke(
        newWalletType: AppWallet.Type,
    ) {
        val activeWallet = App.appCore.session.activeWallet

        if (activeWallet.type == newWalletType) {
            Log.d("The active wallet is already $newWalletType, skipping")
            return
        }

        val allAccounts =
            AccountRepository(App.appCore.session.walletStorage.database.accountDao()).getAll()
        val allIdentities =
            IdentityRepository(App.appCore.session.walletStorage.database.identityDao()).getAll()

        check(allAccounts.isEmpty() && allIdentities.isEmpty()) {
            "Can't switch the wallet type when it is not empty"
        }

        App.appCore.walletRepository.switchWalletType(
            walletId = activeWallet.id,
            newType = newWalletType,
        )
        App.appCore.startNewSession(
            activeWallet = App.appCore.walletRepository.getActiveWallet(),
        )
    }
}
