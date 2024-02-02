package com.concordium.wallet.util

import com.concordium.wallet.data.preferences.AuthPreferences

class KeyCreationVersion(
    private val authPreferences: AuthPreferences,
) {
    /**
     * Whether or not to use V1 methods, requiring the seed.
     */
    val useV1: Boolean
        get() = authPreferences.getHasSetupUser() && authPreferences.hasEncryptedSeed()
}
