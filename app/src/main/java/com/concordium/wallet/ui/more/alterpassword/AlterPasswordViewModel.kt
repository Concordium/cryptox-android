package com.concordium.wallet.ui.more.alterpassword

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlterPasswordViewModel(application: Application) :
    AndroidViewModel(application) {

    private var decryptedMasterKey: ByteArray? = null

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

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

    fun initialize() {
    }

    fun checkLogin(password: String) = viewModelScope.launch(Dispatchers.IO) {
        _waitingLiveData.postValue(true)
        handleAuthPassword(password)
    }

    private suspend fun handleAuthPassword(password: String) {
        // Decrypt the master key
        this.decryptedMasterKey = runCatching {
            App.appCore.authManager.getMasterKey(password)
        }.getOrNull()

        if (decryptedMasterKey == null) {
            _errorLiveData.postValue(Event(R.string.app_error_encryption))
            _waitingLiveData.postValue(false)
            return
        }

        Log.d("starting_auth_reset")

        App.appCore.beginAuthReset()

        _doneInitialAuthenticationLiveData.postValue(Event(true))
        _waitingLiveData.postValue(false)
    }

    fun finishPasswordChange() = viewModelScope.launch {
        _waitingLiveData.postValue(true)

        Log.d("trying_reinit_password_auth")

        try {
            App.appCore.authManager.initPasswordAuth(
                password = App.appCore.session.getPasswordToSetUp()
                    ?: error("Finishing password change while there is no password to set up"),
                masterKey = decryptedMasterKey
                    ?: error("Finishing password change while there is no decrypted master key")
            )

            App.appCore.commitAuthReset()

            _doneFinalChangePasswordLiveData.postValue(Event(true))

            Log.d("committed_auth_reset")
        } catch (e: Exception) {
            Log.e("failed_to_reinit_password_auth", e)

            App.appCore.cancelAuthReset()

            Log.d("cancelled_auth_reset")

            _errorFinalChangePasswordLiveData.postValue(Event(true))
        } finally {
            _waitingLiveData.postValue(false)
        }
    }
}
