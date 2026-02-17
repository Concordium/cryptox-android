package com.concordium.wallet.core.multinetwork

import com.concordium.wallet.App
import com.concordium.wallet.core.AppCore

class SwitchNetworkUseCase {

    /**
     * Switches the app network to the given [newNetwork].
     * Once the network is activated, a new session is started with it.
     * On completion, the main screen must be re-started.
     *
     * @see AppCore.startNewSession
     */
    suspend operator fun invoke(
        newNetwork: AppNetwork,
    ) {
        App.appCore.networkRepository.activate(newNetwork)
        App.appCore.startNewSession(
            network = newNetwork,
        )
    }
}
