package com.concordium.wallet.ui.multinetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.multinetwork.AddNetworkUseCase
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.data.backend.ProxyBackendConfig
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EditNetworkViewModel : ViewModel() {

    private val networkRepository = App.appCore.networkRepository
    private val _eventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = _eventsFlow
    private val _nameInput = MutableStateFlow("")
    private val _nameDefocusEvents = MutableSharedFlow<Boolean>()
    private val _nameError = MutableStateFlow<Error?>(null)
    val nameError: StateFlow<Error?> = _nameError
    private val _validName = MutableStateFlow<String?>(null)
    private val _walletProxyUrlInput = MutableStateFlow("")
    private val _walletProxyUrlDefocusEvents = MutableSharedFlow<Boolean>()
    private val _walletProxyUrlError = MutableStateFlow<Error?>(null)
    val walletProxyUrlError: StateFlow<Error?> = _walletProxyUrlError
    private val _validWalletProxyHttpUrl = MutableStateFlow<HttpUrl?>(null)
    private val _ccdScanUrlInput = MutableStateFlow("")
    private val _ccdScanUrlDefocusEvents = MutableSharedFlow<Boolean>()
    private val _ccdScanUrlError = MutableStateFlow<Error?>(null)
    val ccdScanUrlError: StateFlow<Error?> = _ccdScanUrlError
    private val _validCcdScanHttpUrl = MutableStateFlow<HttpUrl?>(null)
    private val _notificationsServiceUrlInput = MutableStateFlow("")
    private val _notificationsServiceUrlDefocusEvents = MutableSharedFlow<Boolean>()
    private val _notificationsServiceUrlError = MutableStateFlow<Error?>(null)
    val notificationsServiceUrlError: StateFlow<Error?> = _notificationsServiceUrlError
    private val _validNotificationsServiceHttpUrl = MutableStateFlow<HttpUrl?>(null)
    private val _loadedGenesisHash = MutableStateFlow<String?>(null)
    val loadedGenesisHash: StateFlow<String?> = _loadedGenesisHash

    val canSave: StateFlow<Boolean> =
        combine(
            _nameError
                .map { it == null },
            _validName
                .map { it != null },
            _walletProxyUrlError
                .map { it == null },
            _validWalletProxyHttpUrl
                .map { it != null },
            _loadedGenesisHash
                .map { it != null },
            _ccdScanUrlError
                .map { it == null },
            _notificationsServiceUrlError
                .map { it == null },
            transform = { conditions ->
                conditions.all { it }
            }
        )
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        merge(
            _nameInput
                .debounce { if (it.isEmpty()) 0 else 1000 },
            _nameDefocusEvents,
        )
            .map { _nameInput.value }
            .distinctUntilChanged()
            .flatMapLatest {
                validateName()
                flowOf(true)
            }
            .launchIn(viewModelScope)

        merge(
            _walletProxyUrlInput
                .debounce { if (it.isEmpty()) 0 else 1000 },
            _walletProxyUrlDefocusEvents,
        )
            .map { _walletProxyUrlInput.value }
            .distinctUntilChanged()
            .flatMapLatest {
                validateWalletProxyUrlAndLoadGenesisHash()
                flowOf(true)
            }
            .launchIn(viewModelScope)

        merge(
            _ccdScanUrlInput
                .debounce { if (it.isEmpty()) 0 else 1000 },
            _ccdScanUrlDefocusEvents,
        )
            .map { _ccdScanUrlInput.value }
            .distinctUntilChanged()
            .onEach { validateCcdScanUrl() }
            .launchIn(viewModelScope)

        merge(
            _notificationsServiceUrlInput
                .debounce { if (it.isEmpty()) 0 else 1000 },
            _notificationsServiceUrlDefocusEvents,
        )
            .map { _notificationsServiceUrlInput.value }
            .onEach { validateNotificationsServiceUrl() }
            .launchIn(viewModelScope)
    }

    fun onNetworkNameChanged(name: String) {
        _nameInput.value = name
    }

    fun onNetworkNameLostFocus() = viewModelScope.launch {
        _nameDefocusEvents.emit(true)
    }

    fun onWalletProxyUrlChanged(url: String) {
        _walletProxyUrlInput.value = url
    }

    fun onWalletProxyUrlLostFocus() {
        _walletProxyUrlDefocusEvents.tryEmit(true)
    }

    fun onCcdScanUrlChanged(url: String) {
        _ccdScanUrlInput.value = url
    }

    fun onCcdScanUrlLostFocus() {
        _ccdScanUrlDefocusEvents.tryEmit(true)
    }

    fun onNotificationsServiceUrlChanged(url: String) {
        _notificationsServiceUrlInput.value = url
    }

    fun onNotificationsServiceUrlLostFocus() {
        _notificationsServiceUrlDefocusEvents.tryEmit(true)
    }

    private suspend fun validateName() {
        _validName.value = null

        val name = _nameInput
            .value
            .takeIf(String::isNotEmpty)

        if (name == null) {
            _nameError.value = null
            return
        }

        val existingNetwork: AppNetwork? =
            networkRepository
                .getNetworksFlow()
                .first()
                .find { it.name == name }
        if (existingNetwork != null) {
            Log.d("Network already exists: $existingNetwork")
            _nameError.value = Error.NetworkAlreadyExists(existingNetwork)
            return
        }

        _nameError.value = null
        _validName.value = name
    }

    private suspend fun validateWalletProxyUrlAndLoadGenesisHash() {
        _validWalletProxyHttpUrl.value = null
        _loadedGenesisHash.value = null

        // Trailing slashes are mandatory for Retrofit base URLs.
        val walletProxyHttpUrl: HttpUrl = try {
            _walletProxyUrlInput
                .value
                .takeIf(String::isNotEmpty)
                ?.ensureTrailingSlash()
                ?.toHttpUrl()
                ?: return
        } catch (e: Exception) {
            Log.d("Invalid wallet-proxy URL", e)
            _walletProxyUrlError.value = Error.InvalidUrl
            return
        }

        val genesisHash = try {
            getGenesisHash(walletProxyHttpUrl)
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            Log.d("Failed fetching genesis hash", e)
            _walletProxyUrlError.value = Error.BackendError(
                stringRes = BackendErrorHandler.getExceptionStringRes(e)
            )
            return
        }

        _loadedGenesisHash.value = genesisHash

        val existingNetwork: AppNetwork? =
            networkRepository
                .getNetworksFlow()
                .first()
                .find { it.genesisHash == genesisHash }
        if (existingNetwork != null) {
            Log.d("Network already exists: $existingNetwork")
            _walletProxyUrlError.value = Error.NetworkAlreadyExists(existingNetwork)
            return
        }

        _walletProxyUrlError.value = null
        _validWalletProxyHttpUrl.value = walletProxyHttpUrl
    }

    private fun validateCcdScanUrl() {
        _validCcdScanHttpUrl.value = null

        val ccdScanFrontendHttpUrl: HttpUrl? = try {
            _ccdScanUrlInput
                .value
                .takeIf(String::isNotEmpty)
                ?.ensureTrailingSlash()
                ?.toHttpUrl()
        } catch (e: Exception) {
            Log.d("Invalid CCD Scan frontend URL", e)
            _ccdScanUrlError.value = Error.InvalidUrl
            return
        }

        _ccdScanUrlError.value = null
        _validCcdScanHttpUrl.value = ccdScanFrontendHttpUrl
    }

    private fun validateNotificationsServiceUrl() {
        _validNotificationsServiceHttpUrl.value = null

        val notificationsServiceHttpUrl: HttpUrl? = try {
            _notificationsServiceUrlInput
                .value
                .takeIf(String::isNotEmpty)
                ?.ensureTrailingSlash()
                ?.toHttpUrl()
        } catch (e: Exception) {
            Log.d("Invalid notifications service URL", e)
            _notificationsServiceUrlError.value = Error.InvalidUrl
            return
        }

        _notificationsServiceUrlError.value = null
        _validNotificationsServiceHttpUrl.value = notificationsServiceHttpUrl
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

    private var saveJob: Job? = null
    fun onSaveClicked() {
        saveJob?.cancel()
        if (canSave.value) {
            saveJob = viewModelScope.launch {
                save()
            }
        }
    }

    private suspend fun save() {
        try {
            AddNetworkUseCase()
                .invoke(
                    name = _validName.value
                        ?: return,
                    genesisHash = _loadedGenesisHash.value
                        ?: return,
                    walletProxyUrl = _validWalletProxyHttpUrl.value
                        ?: return,
                    ccdScanFrontendUrl = _validCcdScanHttpUrl.value,
                    notificationsServiceUrl = _validNotificationsServiceHttpUrl.value,
                )
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            Log.e("Failed adding network", e)
            _eventsFlow.tryEmit(Event.ShowFloatingError(Error.GenericError))
            return
        }

        _eventsFlow.tryEmit(Event.FinishOnSuccess)
    }

    sealed interface Error {
        object InvalidUrl : Error
        class NetworkAlreadyExists(val existingNetwork: AppNetwork) : Error
        class BackendError(val stringRes: Int) : Error
        object GenericError : Error
    }

    sealed interface Event {
        class ShowFloatingError(val error: Error) : Event
        object FinishOnSuccess : Event
    }
}
