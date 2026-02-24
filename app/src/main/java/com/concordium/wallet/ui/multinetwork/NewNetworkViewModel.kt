package com.concordium.wallet.ui.multinetwork

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.multinetwork.AddAndActivateNetworkUseCase
import com.concordium.wallet.data.backend.ProxyBackendConfig
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class NewNetworkViewModel : ViewModel() {

    private val networkRepository = App.appCore.networkRepository
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _eventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = _eventsFlow

    fun addNetwork(
        name: String,
        walletProxyUrl: String,
        ccdScanFrontendUrl: String?,
        notificationsServiceUrl: String?,
    ) = viewModelScope.launch {
        if (_isLoading.value == true) {
            return@launch
        }

        _isLoading.value = true

        // Trailing slashes are mandatory for Retrofit base URLs.
        val walletProxyHttpUrl: HttpUrl = try {
            walletProxyUrl
                .ensureTrailingSlash()
                .toHttpUrl()
        } catch (e: Exception) {
            Log.d("Invalid wallet-proxy URL", e)
            _eventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidWalletProxyUrl
                )
            )
            _isLoading.value = false
            return@launch
        }

        val ccdScanFrontendHttpUrl: HttpUrl? = try {
            ccdScanFrontendUrl
                ?.ensureTrailingSlash()
                ?.toHttpUrl()
        } catch (e: Exception) {
            ensureActive()
            Log.d("Invalid CCD Scan frontend URL", e)
            _eventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidCcdScanUrl
                )
            )
            _isLoading.value = false
            return@launch
        }

        val notificationsServiceHttpUrl: HttpUrl? = try {
            notificationsServiceUrl
                ?.ensureTrailingSlash()
                ?.toHttpUrl()
        } catch (e: Exception) {
            ensureActive()
            Log.d("Invalid notifications service URL", e)
            _eventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidNotificationsServiceUrl
                )
            )
            _isLoading.value = false
            return@launch
        }

        val genesisHash: String = try {
            getGenesisHash(
                walletProxyUrl = walletProxyHttpUrl,
            )
        } catch (e: Exception) {
            ensureActive()
            Log.d("Failed fetching genesis hash", e)
            _eventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.BackendError(
                        stringRes = BackendErrorHandler.getExceptionStringRes(e)
                    )
                )
            )
            _isLoading.value = false
            return@launch
        }

        val allNetworks = networkRepository.getNetworksFlow().first()
        if (allNetworks.any { it.name == name || it.genesisHash == genesisHash }) {
            Log.d("Network already exists")
            _eventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.NetworkAlreadyExists
                )
            )
            _isLoading.value = false
            return@launch
        }

        AddAndActivateNetworkUseCase()
            .invoke(
                name = name,
                genesisHash = genesisHash,
                walletProxyUrl = walletProxyHttpUrl,
                ccdScanFrontendUrl = ccdScanFrontendHttpUrl,
                notificationsServiceUrl = notificationsServiceHttpUrl,
            )

        Log.d("Added successfully, restarting")

        _eventsFlow.tryEmit(Event.RestartOnSuccess)
    }

    private suspend fun getGenesisHash(walletProxyUrl: HttpUrl): String {
        val proxyBackendConfig = ProxyBackendConfig(
            walletProxyUrl = walletProxyUrl,
            gson = App.appCore.gson,
        )
        return ProxyRepository(proxyBackendConfig.backend)
            .getGenesisHash()
    }

    private fun String.ensureTrailingSlash() =
        trimEnd('/') + '/'

    sealed interface Error {
        object InvalidWalletProxyUrl : Error
        object InvalidCcdScanUrl : Error
        object InvalidNotificationsServiceUrl : Error
        object NetworkAlreadyExists : Error
        class BackendError(val stringRes: Int) : Error
    }

    sealed interface Event {
        class ShowFloatingError(val error: Error) : Event
        object RestartOnSuccess : Event
    }
}
