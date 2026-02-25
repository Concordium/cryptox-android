package com.concordium.wallet.core.multinetwork

import com.concordium.wallet.App
import com.concordium.wallet.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.HttpUrl
import java.util.Date

class AddAndActivateNetworkUseCase {

    /**
     * Adds and activates a new network,
     * having both [name] and [genesisHash] unique.
     * Once added, a new session is started with it.
     * On completion, the main screen must be re-started.
     *
     * @see com.concordium.wallet.core.AppCore.startNewSession
     */
    suspend operator fun invoke(
        name: String,
        genesisHash: String,
        walletProxyUrl: HttpUrl,
        ccdScanFrontendUrl: HttpUrl?,
        notificationsServiceUrl: HttpUrl?,
    ) {
        val newNetwork = AppNetwork(
            name = name,
            genesisHash = genesisHash,
            createdAt = Date(),
            walletProxyUrl = walletProxyUrl,
            ccdScanFrontendUrl = ccdScanFrontendUrl,
            notificationsServiceUrl = notificationsServiceUrl,
            ccdScanBackendUrl = null,
        )

        Log.d("Adding new network: $newNetwork")

        App.Companion.appCore.networkRepository.addAndActivate(newNetwork)
        try {
            FirebaseMessaging.getInstance().deleteToken()
        } catch (error: Exception) {
            Log.e("Failed deleting FCM token", error)
        }
        App.Companion.appCore.startNewSession(
            network = newNetwork,
        )
    }
}
