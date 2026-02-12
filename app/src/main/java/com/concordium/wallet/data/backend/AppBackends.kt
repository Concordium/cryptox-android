package com.concordium.wallet.data.backend

import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.data.backend.airdrop.AirDropBackend
import com.concordium.wallet.data.backend.airdrop.AirDropBackendConfig
import com.concordium.wallet.data.backend.notifications.NotificationsBackend
import com.concordium.wallet.data.backend.notifications.NotificationsBackendConfig
import com.concordium.wallet.data.backend.tokens.TokensBackend
import com.concordium.wallet.data.backend.tokens.TokensBackendConfig
import com.concordium.wallet.data.backend.wert.WertBackend
import com.concordium.wallet.data.backend.wert.WertBackendConfig
import com.google.gson.Gson

class AppBackends(
    private val network: AppNetwork,
    private val gson: Gson,
) {
    val proxyBackendConfig by lazy {
        ProxyBackendConfig(
            walletProxyUrl = network.walletProxyUrl,
            gson = gson,
        )
    }
    val proxy: ProxyBackend
        get() = proxyBackendConfig.backend

    private val tokenBackendConfig by lazy {
        if (network.spacesevenUrl != null)
            TokensBackendConfig(
                spacesevenUrl = network.spacesevenUrl,
                gson = gson,
            )
        else
            null
    }
    val tokens: TokensBackend?
        get() = tokenBackendConfig?.backend

    private val airdropBackendConfig by lazy {
        if (network.spacesevenUrl != null)
            AirDropBackendConfig(
                spacesevenUrl = network.spacesevenUrl,
                gson = gson,
            )
        else
            null
    }
    val airdrop: AirDropBackend?
        get() = airdropBackendConfig?.backend

    private val notificationsBackendConfig by lazy {
        if (network.notificationsServiceUrl != null)
            NotificationsBackendConfig(
                notificationsServiceUrl = network.notificationsServiceUrl,
                gson = gson,
            )
        else
            null
    }
    val notifications: NotificationsBackend?
        get() = notificationsBackendConfig?.backend

    private val wertBackendConfig by lazy {
        WertBackendConfig(gson)
    }
    val wert: WertBackend
        get() = wertBackendConfig.backend
}
