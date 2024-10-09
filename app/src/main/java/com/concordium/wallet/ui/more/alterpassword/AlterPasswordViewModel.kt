package com.concordium.wallet.ui.more.alterpassword

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlterPasswordViewModel(application: Application) :
    AndroidViewModel(application) {

    private var database: WalletDatabase = WalletDatabase.getDatabase(application)
    private var decryptedMasterKey: ByteArray? = null

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _checkAccountsIdentitiesDoneLiveData = MutableLiveData<Boolean>()
    val checkAccountsIdentitiesDoneLiveData: LiveData<Boolean>
        get() = _checkAccountsIdentitiesDoneLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    // Final encryption flow
    private val _doneFinalChangePasswordLiveData = MutableLiveData<Event<Boolean>>()
    val doneFinalChangePasswordLiveData: LiveData<Event<Boolean>>
        get() = _doneFinalChangePasswordLiveData

    private val _errorFinalChangePasswordLiveData = MutableLiveData<Event<Boolean>>()
    val errorFinalChangePasswordLiveData: LiveData<Event<Boolean>>
        get() = _errorFinalChangePasswordLiveData

    // Initial decryption flow
    private val _doneInitialAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val doneInitialAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _doneInitialAuthenticationLiveData

    private val _errorInitialAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val errorInitialAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _errorInitialAuthenticationLiveData

    init {
        val identityDao = database.identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = database.accountDao()
        accountRepository = AccountRepository(accountDao)
    }

    fun initialize() {
    }

    fun checkLogin(password: String) = viewModelScope.launch(Dispatchers.IO) {
        _waitingLiveData.postValue(true)
        handleAuthPassword(password)
    }

    private suspend fun handleAuthPassword(password: String) {
        // Decrypt the master key
        this.decryptedMasterKey = runCatching {
            App.appCore.getCurrentAuthenticationManager().getMasterKey(password)
        }.getOrNull()

        if (decryptedMasterKey == null) {
            _errorLiveData.postValue(Event(R.string.app_error_encryption))
            _waitingLiveData.postValue(false)
            return
        }

        Log.d("starting_reset_auth_flow")

        App.appCore.startResetAuthFlow()

        _doneInitialAuthenticationLiveData.postValue(Event(true))
        _waitingLiveData.postValue(false)
    }

    fun finishPasswordChange(newPassword: String) = viewModelScope.launch {
        _waitingLiveData.postValue(true)

        Log.d("trying_reinit_password_auth")

        try {
            App.appCore.getCurrentAuthenticationManager().initPasswordAuth(
                password = newPassword,
                masterKey = decryptedMasterKey
                    ?: error("Finishing password change while there is no decrypted master key")
            )

            App.appCore.finalizeResetAuthFlow()

            _doneFinalChangePasswordLiveData.postValue(Event(true))

            Log.d("finalized_reset_auth_flow")
        } catch (e: Exception) {
            Log.e("failed_to_reinit_password_auth", e)
            Log.d("cancelling_reset_auth_flow")

            App.appCore.cancelResetAuthFlow()

            _errorFinalChangePasswordLiveData.postValue(Event(true))
        } finally {
            _waitingLiveData.postValue(false)
        }
    }

    fun checkAndStartPasscodeChange() = viewModelScope.launch(Dispatchers.IO) {
        _checkAccountsIdentitiesDoneLiveData.postValue((identityRepository.getNonDoneCount() + accountRepository.getNonDoneCount()) == 0)
    }
}
