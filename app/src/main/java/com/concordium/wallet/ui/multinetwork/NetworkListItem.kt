package com.concordium.wallet.ui.multinetwork

import com.concordium.wallet.core.multinetwork.AppNetwork

sealed interface NetworkListItem {

    class Network(
        val name: String,
        val isConnected: Boolean,
        val source: AppNetwork?,
    ) : NetworkListItem {
        constructor(
            appNetwork: AppNetwork,
            isActive: Boolean,
        ) : this(
            name = appNetwork.name,
            isConnected = isActive,
            source = appNetwork,
        )
    }

    object AddButton : NetworkListItem
}
