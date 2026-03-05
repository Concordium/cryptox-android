package com.concordium.wallet.ui.multinetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.core.multinetwork.SwitchNetworkUseCase
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
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EditNetworkViewModel : ViewModel() {

    private val networkRepository = App.appCore.networkRepository
    private val _eventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = _eventsFlow
    var networkToEdit: AppNetwork? = null
        private set
    private var shouldRestartOnConnect = false
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
    private val genesisHashByWalletProxyUrl = mutableMapOf<HttpUrl, String>()

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

    private var isInitialized = false
    fun init(
        networkToEditHash: String?,
        shouldRestartOnConnect: Boolean,
    ) {
        if (isInitialized) {
            return
        }

        if (networkToEditHash != null) {
            val networkToEdit = runBlocking {
                networkRepository
                    .getNetworksFlow()
                    .first()
                    .find { it.genesisHash == networkToEditHash }
                    ?: error("Network $networkToEditHash not found")
            }
            _loadedGenesisHash.value = networkToEdit.genesisHash
            genesisHashByWalletProxyUrl[networkToEdit.walletProxyUrl] = networkToEdit.genesisHash
            this.networkToEdit = networkToEdit
        }

        this.shouldRestartOnConnect = shouldRestartOnConnect
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
        if (existingNetwork != null && existingNetwork != networkToEdit) {
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
        if (existingNetwork != null && existingNetwork != networkToEdit) {
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
        if (genesisHashByWalletProxyUrl.containsKey(walletProxyUrl)) {
            return genesisHashByWalletProxyUrl.getValue(walletProxyUrl)
        }

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
        saveJob = viewModelScope.launch {
            validateName()
            validateWalletProxyUrlAndLoadGenesisHash()
            validateCcdScanUrl()
            validateNotificationsServiceUrl()

            if (!canSave.value) {
                return@launch
            }

            if (networkToEdit == null) {
                addNewNetwork()
            } else {
                updateNetworkToEdit()
            }
        }
    }

    private suspend fun addNewNetwork() {
        val addedNetwork: AppNetwork

        try {
            addedNetwork =
                networkRepository
                    .addInactive(
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
            Log.e("Failed adding network", e)
            _eventsFlow.tryEmit(Event.ShowFloatingError(Error.GenericError))
            return
        }

        _eventsFlow.tryEmit(
            Event.FinishAfterAdded(
                addedNetworkName = addedNetwork.name,
            )
        )
    }

    private suspend fun updateNetworkToEdit() {
        val updatedNetwork: AppNetwork
        var isReconnected = false

        try {
            updatedNetwork =
                networkRepository
                    .update(
                        currentGenesisHash = networkToEdit!!.genesisHash,
                        newGenesisHash = _loadedGenesisHash.value
                            ?: return,
                        name = _validName.value
                            ?: return,
                        walletProxyUrl = _validWalletProxyHttpUrl.value
                            ?: return,
                        ccdScanFrontendUrl = _validCcdScanHttpUrl.value,
                        notificationsServiceUrl = _validNotificationsServiceHttpUrl.value,
                    )

            if (networkToEdit == App.appCore.session.network) {
                SwitchNetworkUseCase()
                    .invoke(
                        newNetwork = updatedNetwork,
                    )
                isReconnected = true
            }
        } catch (e: Exception) {
            Log.e("Failed updating network", e)
            _eventsFlow.tryEmit(Event.ShowFloatingError(Error.GenericError))
            return
        }

        _eventsFlow.tryEmit(
            if (isReconnected && shouldRestartOnConnect)
                Event.RestartAfterEdited(
                    editedNetworkName = updatedNetwork.name,
                )
            else
                Event.FinishAfterEdited(
                    editedNetworkName = updatedNetwork.name,
                )
        )
    }

    sealed interface Error {
        object InvalidUrl : Error
        class NetworkAlreadyExists(val existingNetwork: AppNetwork) : Error
        class BackendError(val stringRes: Int) : Error
        object GenericError : Error
    }

    sealed interface Event {
        class ShowFloatingError(
            val error: Error,
        ) : Event

        class FinishAfterAdded(
            val addedNetworkName: String,
        ) : Event

        class RestartAfterEdited(
            val editedNetworkName: String,
        ) : Event

        class FinishAfterEdited(
            val editedNetworkName: String,
        ) : Event
    }
}
