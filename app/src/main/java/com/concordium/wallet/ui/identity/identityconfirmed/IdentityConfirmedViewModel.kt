package com.concordium.wallet.ui.identity.identityconfirmed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.identity.IdentityUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class IdentityErrorData(
    val identity: Identity,
    val account: Account?,
    val isFirstIdentity: Boolean
)

class IdentityConfirmedViewModel(application: Application) : AndroidViewModel(application) {
    private val identityRepository: IdentityRepository
    private val identityUpdater = IdentityUpdater(application, viewModelScope)

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _isFirstIdentityLiveData = MutableLiveData<Boolean>()
    val isFirstIdentityLiveData: LiveData<Boolean>
        get() = _isFirstIdentityLiveData

    private val _newFinalizedAccountLiveData = MutableLiveData<String>()
    val newFinalizedAccountLiveData: LiveData<String>
        get() = _newFinalizedAccountLiveData

    private val _identityErrorLiveData = MutableLiveData<IdentityErrorData>()
    val identityErrorLiveData: LiveData<IdentityErrorData>
        get() = _identityErrorLiveData

    lateinit var identity: Identity

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    override fun onCleared() {
        super.onCleared()
        identityUpdater.stop()
    }

    fun startIdentityUpdate() {
        val updateListener = object : IdentityUpdater.UpdateListener {
            override fun onError(identity: Identity, account: Account?) {
                val isFirstIdentity = isFirstIdentityLiveData.value
                viewModelScope.launch(Dispatchers.IO) {
                    val isFirst = isFirstIdentity ?: isFirst(identityRepository.getCount())
                    _identityErrorLiveData.postValue(IdentityErrorData(identity, account, isFirst))
                }
            }

            override fun onDone() {
            }

            override fun onNewAccountFinalized(accountName: String) {
                viewModelScope.launch(Dispatchers.IO) {
                    _newFinalizedAccountLiveData.postValue(accountName)
                    App.appCore.session.setAccountsBackedUp(false)
                }
            }
        }
        identityUpdater.checkPendingIdentities(updateListener)
    }

    fun stopIdentityUpdate() {
        identityUpdater.stop()
    }

    fun updateState() {
        _waitingLiveData.value = true
        viewModelScope.launch {
            val identityCount = identityRepository.getCount()
            _waitingLiveData.postValue(false)
            // If we are in the process of creating the first identity, there will be one identity saved at this point
            _isFirstIdentityLiveData.value = isFirst(identityCount)
        }
    }

    suspend fun getIdentityFromId(id: Int): Identity? {
        return identityRepository.findById(id)
    }

    private fun isFirst(identityCount: Int): Boolean {
        // If we are in the process of creating the first identity, there will be one identity saved at this point
        return (identityCount <= 1)
    }
}
