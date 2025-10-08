package com.concordium.wallet.ui.seed.setup

import com.concordium.wallet.App
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.core.multiwallet.SwitchActiveWalletTypeUseCase

class SetUpSeedPhraseWalletUseCase {

    /**
     * Sets up the current wallet as a [AppWallet.Type.SEED] wallet with the given [seedPhraseString]
     * encrypted with the given [password].
     * If [isBackupConfirmed] is false, the user will later see the backup banner.
     *
     * @return **true** if the setup succeeded.
     */
    suspend operator fun invoke(
        seedPhraseString: String,
        password: String,
        isBackupConfirmed: Boolean,
    ): Boolean {

        check(!App.appCore.session.walletStorage.setupPreferences.hasEncryptedSeed()) {
            "Trying setting up a seed phrase wallet, yet there already is an encrypted seed " +
                    "which is not expected"
        }

        val isSavedSuccessfully =
            App
                .appCore
                .session
                .walletStorage
                .setupPreferences
                .tryToSetEncryptedSeedPhrase(
                    seedPhraseString = seedPhraseString,
                    password = password,
                )

        if (isSavedSuccessfully) {
            SwitchActiveWalletTypeUseCase().invoke(
                newWalletType = AppWallet.Type.SEED,
            )
            App
                .appCore
                .session
                .walletStorage
                .setupPreferences
                .setRequireSeedPhraseBackupConfirmation(!isBackupConfirmed)
        }

        return isSavedSuccessfully
    }
}
