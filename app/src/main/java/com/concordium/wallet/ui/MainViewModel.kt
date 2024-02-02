package com.concordium.wallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.identity.IdentityUpdater
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityErrorData
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    enum class State {
        AccountOverview,
        TokensOverview,
        More,
        ;
    }

    private val identityRepository: IdentityRepository
    private val identityUpdater = IdentityUpdater(application, viewModelScope)
    private val session: Session = App.appCore.session

    var databaseVersionAllowed = true

    private val _newFinalizedAccountLiveData = MutableLiveData<String>()
    val newFinalizedAccountLiveData: LiveData<String>
        get() = _newFinalizedAccountLiveData

    private val _titleLiveData = MutableLiveData<String>()
    val titleLiveData: LiveData<String>
        get() = _titleLiveData

    private val _stateLiveData = MutableLiveData<State>()
    val stateLiveData: LiveData<State>
        get() = _stateLiveData

    private val _identityErrorLiveData = MutableLiveData<IdentityErrorData>()
    val identityErrorLiveData: LiveData<IdentityErrorData>
        get() = _identityErrorLiveData

    val canAcceptImportFiles: Boolean
        get() = App.appCore.session.isAccountsBackupPossible()

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
    }

    fun initialize() {
        try {
            val dbVersion =
                WalletDatabase.getDatabase(getApplication()).openHelper.readableDatabase.version.toString()
        } catch (e: Exception) {
            Log.e("Database init failed. Missing migrations?")
            e.printStackTrace()
            databaseVersionAllowed = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        identityUpdater.stop()
    }

    fun setTitle(title: String) {
        _titleLiveData.postValue(title)
    }

    fun setState(state: State) {
        _stateLiveData.postValue(state)
    }

    fun setInitialStateIfNotSet() {
        if (_stateLiveData.value == null) {
            _stateLiveData.postValue(State.AccountOverview)
        }
    }

    fun shouldShowAuthentication(): Boolean {
        session.isLoggedIn.value.let { return it == null || it == false }
    }

    fun shouldShowPasswordSetup(): Boolean {
        return !session.hasSetupPassword
    }

    fun shouldShowInitialSetup(): Boolean {
        return session.hasSetupPassword && !session.hasCompletedInitialSetup
    }

    fun startIdentityUpdate() {
        val updateListener = object : IdentityUpdater.UpdateListener {
            override fun onError(identity: Identity, account: Account?) {
                viewModelScope.launch(Dispatchers.IO) {
                    val isFirst = isFirst(identityRepository.getCount())
                    _identityErrorLiveData.postValue(IdentityErrorData(identity, account, isFirst))
                }
            }

            override fun onDone() {
            }

            override fun onNewAccountFinalized(accountName: String) {
                viewModelScope.launch {
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

    private fun isFirst(identityCount: Int): Boolean {
        // If we are in the process of creating the first identity, there will be one identity saved at this point
        return (identityCount <= 1)
    }
}
