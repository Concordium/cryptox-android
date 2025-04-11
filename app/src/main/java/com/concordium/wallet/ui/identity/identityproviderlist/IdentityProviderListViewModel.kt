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
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.cryptolib.IdRequestAndPrivateDataOutputV1
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.IdentityCreationData
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.ui.common.BackendErrorHandler
import kotlinx.coroutines.launch

class IdentityProviderListViewModel(application: Application) : AndroidViewModel(application) {
    private val identityRepository =
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    private val repository = IdentityProviderRepository()

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
        var identityIndex = 0
        var identityName = ""
    }

    init {
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
        // Create private data based on seed phrase.
        val output = createIdRequestAndPrivateDataV1(password)
        if (output != null) {
            tempData.idObjectRequest = output.idObjectRequest
            _gotoIdentityProviderWebView.postValue(Event(true))
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
            _waitingLiveData.value = false
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
        val seed = App.appCore.session.walletStorage.setupPreferences.getSeedHex(password)

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

        if (identityProvider == null || idObjectRequest == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return null
        }

        return IdentityCreationData(
            identityProvider = identityProvider,
            idObjectRequest = idObjectRequest,
            identityName = tempData.identityName,
            identityIndex = tempData.identityIndex
        )
    }

    fun checkNotFileWallet() =
        check(App.appCore.session.activeWallet.type != AppWallet.Type.FILE) {
            "File wallet can't be used to perform this action"
        }
}
