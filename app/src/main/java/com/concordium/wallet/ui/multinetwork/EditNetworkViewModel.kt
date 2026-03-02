package com.concordium.wallet.ui.multinetwork

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.concordium.wallet.App
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.data.backend.ProxyBackendConfig
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class EditNetworkViewModel : ViewModel() {

    private val networkRepository = App.appCore.networkRepository
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _eventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = _eventsFlow
    private val _nameInput = MutableStateFlow("")
    private val _walletProxyUrlInput = MutableStateFlow("")
    private val _ccdScanUrlInput = MutableStateFlow("")
    private val _notificationsServiceUrlInput = MutableStateFlow("")
    private val _canSave = MutableStateFlow(false)
    val canSave: Flow<Boolean> = _canSave

    fun onNetworkNameChanged(name: String) {
        _nameInput.value = name
    }

    fun onWalletProxyUrlChanged(url: String) {
        _walletProxyUrlInput.value = url
    }

    fun onCcdScanUrlChanged(url: String) {
        _ccdScanUrlInput.value = url
    }

    fun onNotificationsServiceUrlChanged(url: String) {
        _notificationsServiceUrlInput.value = url
    }

    private suspend fun getDataToSave(
        name: String,
        walletProxyUrl: String,
        ccdScanFrontendUrl: String?,
        notificationsServiceUrl: String?,
    ): Pair<DataToSave?, Error?> {
        // Trailing slashes are mandatory for Retrofit base URLs.
        val walletProxyHttpUrl: HttpUrl = try {
            walletProxyUrl
                .ensureTrailingSlash()
                .toHttpUrl()
        } catch (e: Exception) {
            Log.d("Invalid wallet-proxy URL", e)
            return null to Error.InvalidWalletProxyUrl
        }

        val ccdScanFrontendHttpUrl: HttpUrl? = try {
            ccdScanFrontendUrl
                ?.ensureTrailingSlash()
                ?.toHttpUrl()
        } catch (e: Exception) {
            Log.d("Invalid CCD Scan frontend URL", e)
            return null to Error.InvalidCcdScanUrl
        }

        val notificationsServiceHttpUrl: HttpUrl? = try {
            notificationsServiceUrl
                ?.ensureTrailingSlash()
                ?.toHttpUrl()
        } catch (e: Exception) {
            Log.d("Invalid notifications service URL", e)
            return null to Error.InvalidNotificationsServiceUrl
        }

        val genesisHash: String = try {
            getGenesisHash(
                walletProxyUrl = walletProxyHttpUrl,
            )
        } catch (e: Exception) {
            Log.d("Failed fetching genesis hash", e)
            return null to Error.BackendError(
                stringRes = BackendErrorHandler.getExceptionStringRes(e)
            )
        }

        val existingNetwork: AppNetwork? =
            networkRepository
                .getNetworksFlow()
                .first()
                .find { it.name == name || it.genesisHash == genesisHash }
        if (existingNetwork != null) {
            Log.d("Network already exists: $existingNetwork")
            return null to Error.NetworkAlreadyExists(existingNetwork)
        }

        return DataToSave(
            name = name,
            walletProxyUrl = walletProxyHttpUrl,
            genesisHash = genesisHash,
            ccdScanFrontendUrl = ccdScanFrontendHttpUrl,
            notificationsServiceUrl = notificationsServiceHttpUrl,
        ) to null
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

    private class DataToSave(
        val name: String,
        val walletProxyUrl: HttpUrl,
        val genesisHash: String,
        val ccdScanFrontendUrl: HttpUrl?,
        val notificationsServiceUrl: HttpUrl?,
    )

    sealed interface Error {
        object InvalidWalletProxyUrl : Error
        object InvalidCcdScanUrl : Error
        object InvalidNotificationsServiceUrl : Error
        class NetworkAlreadyExists(val existingNetwork: AppNetwork) : Error
        class BackendError(val stringRes: Int) : Error
    }

    sealed interface Event {
        class ShowFloatingError(val error: Error) : Event
        object RestartOnSuccess : Event
    }
}
