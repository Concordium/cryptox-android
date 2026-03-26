package com.concordium.wallet.core.multinetwork

import com.concordium.wallet.App
import com.concordium.wallet.core.AppCore
import com.concordium.wallet.util.Log
import com.google.firebase.messaging.FirebaseMessaging

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
        try {
            FirebaseMessaging.getInstance().deleteToken()
        } catch (error: Exception) {
            Log.e("failed_deleting_notification_token", error)
        }
        App.appCore.networkRepository.activate(newNetwork)
        App.appCore.startNewSession(
            network = newNetwork,
        )
    }
}
