package com.concordium.wallet.core.multinetwork

import com.concordium.wallet.App
import com.concordium.wallet.util.Log
import okhttp3.HttpUrl
import java.util.Date

class AddNetworkUseCase {

    /**
     * Adds and activates a new network,
     * having both [name] and [genesisHash] unique.
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

        Log.d("Adding new inactive network: $newNetwork")

        App.Companion.appCore.networkRepository.addInactive(newNetwork)
    }
}
