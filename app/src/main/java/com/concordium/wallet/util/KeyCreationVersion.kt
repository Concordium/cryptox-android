package com.concordium.wallet.util

import com.concordium.wallet.data.preferences.WalletSetupPreferences

class KeyCreationVersion(
    private val walletSetupPreferences: WalletSetupPreferences,
) {
    /**
     * Whether or not to use V1 methods, requiring the seed.
     */
    val useV1: Boolean
        get() = walletSetupPreferences.getHasSetupUser() && walletSetupPreferences.hasEncryptedSeed()
}
