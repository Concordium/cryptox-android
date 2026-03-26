package com.concordium.wallet.ui.multinetwork

import com.concordium.wallet.core.multinetwork.AppNetwork

sealed interface NetworkListItem {

    class Network(
        val name: String,
        val isConnected: Boolean,
        val isEditing: Boolean,
        val isEditable: Boolean,
        val source: AppNetwork?,
    ) : NetworkListItem {
        constructor(
            appNetwork: AppNetwork,
            isActive: Boolean,
            isEditing: Boolean,
        ) : this(
            name = appNetwork.name,
            isConnected = isActive,
            isEditing = isEditing,
            isEditable = !appNetwork.isPermanent,
            source = appNetwork,
        )
    }

    object AddButton : NetworkListItem
}
