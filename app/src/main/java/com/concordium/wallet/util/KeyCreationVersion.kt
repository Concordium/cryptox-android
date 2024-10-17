package com.concordium.wallet.util

import com.concordium.wallet.core.multiwallet.AppWallet

class KeyCreationVersion(
    private val activeWallet: AppWallet,
) {
    /**
     * Whether or not to use V1 methods, requiring the seed.
     */
    val useV1: Boolean
        get() = activeWallet.type != AppWallet.Type.FILE
}
