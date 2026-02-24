package com.concordium.wallet.ui.multinetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.multinetwork.AppNetwork
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NetworksViewModel : ViewModel() {

    private val networkRepository = App.appCore.networkRepository
    private val activeNetwork = App.appCore.session.network

    val items: StateFlow<List<NetworkListItem>> =
        networkRepository
            .getNetworksFlow()
            .map { networks ->
                networks
                    .sortedBy(AppNetwork::createdAt)
                    .map { appNetwork ->
                        NetworkListItem.Network(
                            appNetwork = appNetwork,
                            isActive = activeNetwork == appNetwork,
                        )
                    } + NetworkListItem.AddButton
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
