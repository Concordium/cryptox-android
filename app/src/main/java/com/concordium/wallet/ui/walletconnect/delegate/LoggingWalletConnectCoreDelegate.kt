package com.concordium.wallet.ui.walletconnect.delegate

import com.concordium.wallet.util.Log
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient

open class LoggingWalletConnectCoreDelegate: CoreClient.CoreDelegate {
    override fun onPairingDelete(deletedPairing: Core.Model.DeletedPairing) {
        Log.d(
            "pairing_deleted:" +
                    "\ndeleted=$deletedPairing"
        )
    }
}
