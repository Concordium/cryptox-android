package com.concordium.wallet.ui.identity.identityproviderlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.cryptolib.IdRequestAndPrivateDataOutput
import com.concordium.wallet.data.cryptolib.IdRequestAndPrivateDataOutputV1
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.IdentityCreationData
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.KeyCreationVersion
import kotlinx.coroutines.launch

class IdentityProviderListViewModel(application: Application) : AndroidViewModel(application) {
    private val identityRepository: IdentityRepository
    private val repository: IdentityProviderRepository = IdentityProviderRepository()
    private val gson = App.appCore.gson
    private val keyCreationVersion = KeyCreationVersion(AuthPreferences(App.appContext))

    private var identityProviderInfoRequest: BackendRequest<ArrayList<IdentityProvider>>? = null
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null

    private val tempData = TempData()
    private var currentIdentityProvider: IdentityProvider? = null

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _waitingGlobalData = MutableLiveData<Boolean>()
    val waitingGlobalData: LiveData<Boolean>
        get() = _waitingGlobalData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _identityProviderList = MutableLiveData<List<IdentityProvider>>()
    val identityProviderList: LiveData<List<IdentityProvider>>
        get() = _identityProviderList

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _gotoIdentityProviderWebView = MutableLiveData<Event<Boolean>>()
    val gotoIdentityProviderWebView: LiveData<Event<Boolean>>
        get() = _gotoIdentityProviderWebView

    private class TempData {
        var globalParams: GlobalParams? = null
        var identityProvider: IdentityProvider? = null
        var idObjectRequest: RawJson? = null
        var privateIdObjectDataEncrypted: String? = null
        var identityIndex = 0
        var identityName = ""
        var encryptedAccountData: String? = null
        var accountAddress: String? = null
    }

    init {
        App.appCore.tracker.identityVerificationProvidersListScreen()
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        _waitingLiveData.value = true
        _waitingGlobalData.value = true
    }

    override fun onCleared() {
        super.onCleared()
        identityProviderInfoRequest?.dispose()
    }

    fun getIdentityProviders() {
        _waitingLiveData.value = true
        identityProviderInfoRequest?.dispose()
        identityProviderInfoRequest = repository.getIdentityProviderInfo(
            useLegacy = !keyCreationVersion.useV1,
            {
                _identityProviderList.value = it
                _waitingLiveData.value = false
            },
            {
                _waitingLiveData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
            }
        )
    }

    fun getGlobalInfo() {
        _waitingGlobalData.value = true
        globalParamsRequest?.dispose()
        globalParamsRequest = repository.getIGlobalInfo(
            {
                tempData.globalParams = it.value
                _waitingGlobalData.value = false
            },
            {
                _waitingGlobalData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
            }
        )
    }

    fun selectedIdentityVerificationItem(identityProvider: IdentityProvider) {
        currentIdentityProvider = identityProvider
        tempData.identityProvider = identityProvider
        _showAuthenticationLiveData.value = Event(true)
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        _waitingLiveData.value = true
        encryptAndContinue(password)
    }

    private suspend fun encryptAndContinue(password: String) {
        if (keyCreationVersion.useV1) {
            // Create private data based on seed phrase.
            val output = createIdRequestAndPrivateDataV1(password)
            if (output != null) {
                tempData.idObjectRequest = output.idObjectRequest
                _gotoIdentityProviderWebView.postValue(Event(true))
            } else {
                _errorLiveData.value = Event(R.string.app_error_encryption)
                _waitingLiveData.value = false
            }
        } else {
            // Create and encrypt the private data in the legacy way.
            val output = createIdRequestAndPrivateData()
            if (output != null) {
                val tempCurrentPrivateIdObjectDataJson =
                    gson.toJson(output.privateIdObjectData.value)
                val encodedEncrypted =
                    App.appCore.getCurrentAuthenticationManager().encryptInBackground(
                        password,
                        tempCurrentPrivateIdObjectDataJson
                    )
                if (encodedEncrypted != null && encryptAccountData(password, output)) {
                    tempData.privateIdObjectDataEncrypted = encodedEncrypted
                    tempData.idObjectRequest = output.idObjectRequest
                    tempData.accountAddress = output.initialAccountData.accountAddress
                    _gotoIdentityProviderWebView.postValue(Event(true))
                } else {
                    _errorLiveData.postValue(Event(R.string.app_error_encryption))
                    _waitingLiveData.postValue(false)
                }
            }
        }
    }

    private suspend fun encryptAccountData(
        password: String,
        output: IdRequestAndPrivateDataOutput
    ): Boolean {
        // Encrypt account data for later when saving account
        val initialAccountData = output.initialAccountData
        val jsonToBeEncrypted = gson.toJson(initialAccountData)
        val storageAccountDataEncrypted = App.appCore.getCurrentAuthenticationManager()
            .encryptInBackground(password, jsonToBeEncrypted)
        if (storageAccountDataEncrypted != null) {
            tempData.encryptedAccountData = storageAccountDataEncrypted
            return true
        }
        return false
    }

    private suspend fun createIdRequestAndPrivateData(): IdRequestAndPrivateDataOutput? {
        val identityProvider = tempData.identityProvider
        val global = tempData.globalParams
        if (identityProvider == null) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            _waitingLiveData.postValue(false)
            return null
        }

        tempData.identityIndex =
            identityRepository.nextIdentityIndex(identityProvider.ipInfo.ipIdentity)
        tempData.identityName =
            identityRepository.nextIdentityName(getApplication<Application>().getString(R.string.identity_create_default_name_prefix))

        val output =
            App.appCore.cryptoLibrary.createIdRequestAndPrivateData(
                identityProvider.ipInfo,
                identityProvider.arsInfos,
                global
            )
        return if (output != null) {
            output
        } else {
            _errorLiveData.postValue(Event(R.string.app_error_lib))
            _waitingLiveData.postValue(false)
            null
        }
    }

    private suspend fun createIdRequestAndPrivateDataV1(password: String): IdRequestAndPrivateDataOutputV1? {
        val identityProvider = tempData.identityProvider
        val global = tempData.globalParams
        if (identityProvider == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return null
        }

        val net = AppConfig.net
        tempData.identityIndex =
            identityRepository.nextIdentityIndex(identityProvider.ipInfo.ipIdentity)
        tempData.identityName =
            identityRepository.nextIdentityName(getApplication<Application>().getString(R.string.identity_create_default_name_prefix))
        val seed = AuthPreferences(getApplication()).getSeedHex(password)

        val output = App.appCore.cryptoLibrary.createIdRequestAndPrivateDataV1(
            identityProvider.ipInfo,
            identityProvider.arsInfos,
            global,
            seed,
            net,
            tempData.identityIndex
        )
        return if (output != null) {
            output
        } else {
            _errorLiveData.value = Event(R.string.app_error_lib)
            _waitingLiveData.value = false
            null
        }
    }

    fun getIdentityCreationData(): IdentityCreationData? {
        val identityProvider = tempData.identityProvider
        val idObjectRequest = tempData.idObjectRequest

        if (keyCreationVersion.useV1) {
            if (identityProvider == null || idObjectRequest == null) {
                _errorLiveData.value = Event(R.string.app_error_general)
                return null
            }
            return IdentityCreationData.V1(
                identityProvider,
                idObjectRequest,
                tempData.identityName,
                tempData.identityIndex
            )
        } else {
            val privateIdObjectDataEncrypted = tempData.privateIdObjectDataEncrypted
            val encryptedAccountData = tempData.encryptedAccountData
            val accountAddress = tempData.accountAddress

            if (identityProvider == null || idObjectRequest == null || privateIdObjectDataEncrypted == null || encryptedAccountData == null || accountAddress == null) {
                _errorLiveData.postValue(Event(R.string.app_error_general))
                return null
            }
            return IdentityCreationData.V0(
                privateIdObjectDataEncrypted,
                "",
                encryptedAccountData,
                accountAddress,
                identityProvider,
                idObjectRequest,
                tempData.identityName,
                tempData.identityIndex,
            )
        }
    }

    fun checkUsingV1KeyCreation() =
        check(keyCreationVersion.useV1) {
            "Key creation V1 (seed-based) must be used to perform this action"
        }
}
