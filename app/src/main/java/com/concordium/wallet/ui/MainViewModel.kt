package com.concordium.wallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.common.identity.IdentityUpdater
import com.concordium.wallet.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    enum class State {
        AccountOverview,
        NewsOverview,
        More,
        ;
    }

    private val identityUpdater = IdentityUpdater(application, viewModelScope)

    var databaseVersionAllowed = true

    private val _titleLiveData = MutableLiveData<String>()
    val titleLiveData: LiveData<String>
        get() = _titleLiveData

    private val _stateLiveData = MutableLiveData<State>()
    val stateLiveData: LiveData<State>
        get() = _stateLiveData

    private val _notificationTokenId = MutableStateFlow("")
    val notificationTokenId = _notificationTokenId.asStateFlow()

    private val _activeAccountAddress = MutableStateFlow("")
    val activeAccountAddress = _activeAccountAddress.asStateFlow()

    private val _showReviewDialog = MutableLiveData<Event<Boolean>>()
    val showReviewDialog: LiveData<Event<Boolean>> = _showReviewDialog

    val canAcceptImportFiles: Boolean
        get() = App.appCore.session.isAccountsBackupPossible()

    fun initialize() {
        try {
            val dbVersion =
                App.appCore.session.walletStorage.database.openHelper.readableDatabase.version.toString()
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
        App.appCore.session.isLoggedIn.value.let { return it == null || it == false }
    }

    fun shouldShowPasswordSetup(): Boolean {
        return !App.appCore.setup.isAuthSetupCompleted
    }

    fun shouldShowInitialSetup(): Boolean {
        return !App.appCore.setup.isInitialSetupCompleted
    }

    fun hasCompletedOnboarding(): Boolean {
        return App.appCore.session.walletStorage.setupPreferences.getHasCompletedOnboarding()
    }

    fun activateAccount(address: String) {
        val accountRepository =
            AccountRepository(App.appCore.session.walletStorage.database.accountDao())

        viewModelScope.launch {
            accountRepository.activate(address)
        }
    }

    fun setNotificationData(address: String, id: String) = viewModelScope.launch {
        _activeAccountAddress.emit(address)
        _notificationTokenId.emit(id)
    }

    fun showReviewDialog() {
        _showReviewDialog.postValue(Event(true))
    }

    fun startIdentityUpdate() {
        val updateListener = object : IdentityUpdater.UpdateListener {
            override fun onError(identity: Identity, account: Account?) {
            }

            override fun onDone() {
            }

            override fun onNewAccountFinalized(accountName: String) {
                viewModelScope.launch {
                    App.appCore.session.setAccountsBackedUp(false)
                }
            }
        }
        identityUpdater.checkPendingIdentities(updateListener)
    }

    fun stopIdentityUpdate() {
        identityUpdater.stop()
    }
}
