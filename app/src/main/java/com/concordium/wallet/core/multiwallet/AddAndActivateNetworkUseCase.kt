package com.concordium.wallet.core.multiwallet

import com.concordium.wallet.App
import com.concordium.wallet.core.AppCore
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Date

class AddAndActivateNetworkUseCase {

    /**
     * Adds and activates a new network,
     * having both [name] and [genesisHash] unique.
     * Once added, a new session is started with it.
     * On completion, the main screen must be re-started.
     *
     * @see AppCore.startNewSession
     */
    suspend operator fun invoke(
        name: String,
        // TODO fetch from WalletProxy.
        genesisHash: String,
        walletProxyUrl: String,
        ccdScanFrontendUrl: String?,
        notificationsServiceUrl: String?,
    ) {
        // Trailing slashes are mandatory for Retrofit base URLs.
        val newNetwork = AppNetwork(
            name = name,
            genesisHash = genesisHash,
            createdAt = Date(),
            walletProxyUrl =
                walletProxyUrl
                    .ensureTrailingSlash()
                    .toHttpUrl(),
            ccdScanFrontendUrl =
                ccdScanFrontendUrl
                    ?.ensureTrailingSlash()
                    ?.toHttpUrl(),
            notificationsServiceUrl =
                notificationsServiceUrl
                    ?.ensureTrailingSlash()
                    ?.toHttpUrl(),
            ccdScanBackendUrl = null,
            spacesevenUrl = null,
        )

        try {
            FirebaseMessaging.getInstance().deleteToken()
        } catch (error: Exception) {
            Log.e("failed_deleting_notification_token", error)
        }
        App.appCore.networkRepository.addAndActivate(newNetwork)
        App.appCore.startNewSession(
            network = newNetwork,
        )
    }

    private fun String.ensureTrailingSlash() =
        trimEnd('/') + '/'
}
