package com.concordium.wallet.ui.walletconnect.delegate

import com.concordium.wallet.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient

open class LoggingWalletConnectCoreDelegate: CoreClient.CoreDelegate {
    override fun onPairingDelete(deletedPairing: Core.Model.DeletedPairing) {
        Log.d(
            "pairing_deleted:" +
                    "\ndeleted=$deletedPairing"
        )
    }
}
