package com.concordium.wallet.ui.walletconnect.delegate

import com.concordium.wallet.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient

open class LoggingWalletConnectCoreDelegate : CoreClient.CoreDelegate {
    override fun onPairingState(pairingState: Core.Model.PairingState) {
        Log.d(
            "pairing_state_changed:" +
                    "\nstate=${pairingState.isPairingState}"
        )
    }
}
