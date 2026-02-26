package com.concordium.wallet.ui.multinetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.core.multinetwork.SwitchNetworkUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetworksViewModel : ViewModel() {

    private val networkRepository = App.appCore.networkRepository
    private val activeNetwork = App.appCore.session.network
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing
    private val _eventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = _eventsFlow

    val items: StateFlow<List<NetworkListItem>> =
        combine(
            networkRepository
                .getNetworksFlow(),
            isEditing,
            ::Pair
        )
            .map { (networks, isEditing) ->
                networks
                    .sortedBy(AppNetwork::createdAt)
                    .map { appNetwork ->
                        NetworkListItem.Network(
                            appNetwork = appNetwork,
                            isActive = activeNetwork == appNetwork,
                            isEditing = isEditing,
                        )
                    } + NetworkListItem.AddButton
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onNetworkItemClicked(item: NetworkListItem.Network) {
        val network = item.source
            ?: return

        if (isEditing.value && !network.isPermanent) {
            _eventsFlow.tryEmit(Event.GoToEdit(network))
        } else if (!isEditing.value && network != activeNetwork) {
            viewModelScope.launch {
                SwitchNetworkUseCase()
                    .invoke(
                        newNetwork = network,
                    )
            }
        }
    }

    fun onAddClicked() {
        _eventsFlow.tryEmit(Event.GoToEdit(null))
    }

    fun onEditClicked() {
        _isEditing.value = true
    }

    fun onDoneClicked() {
        _isEditing.value = false
    }

    sealed interface Event {
        class GoToEdit(
            val network: AppNetwork?,
        ) : Event
    }
}
