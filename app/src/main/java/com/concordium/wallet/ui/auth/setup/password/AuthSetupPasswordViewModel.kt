package com.concordium.wallet.ui.auth.setup.password

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.authentication.Session
import kotlinx.coroutines.launch

class AuthSetupPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val session: Session = App.appCore.session

    private val _errorLiveData = MutableLiveData<Event<Boolean>>()
    val errorLiveData: LiveData<Event<Boolean>>
        get() = _errorLiveData
    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData

    fun initialize() {
    }

    fun startSetupPassword(password: String) {
        // Keep password for re-enter check and biometrics activation (outside this screen).
        session.startPasswordSetup(password)
    }

    fun checkPasswordRequirements(password: String): Boolean {
        return (password.length >= 6)
    }

    fun setupPassword(password: String) = viewModelScope.launch {
        val isSetUpSuccessfully = runCatching {
            val authResetMasterKey = App.appCore.authResetMasterKey
            if (authResetMasterKey != null) {
                App.appCore.authManager.initPasswordAuth(
                    password = password,
                    masterKey = authResetMasterKey,
                )
            } else {
                App.appCore.authManager.initPasswordAuth(
                    password = password,
                )
            }
        }.isSuccess

        if (isSetUpSuccessfully) {
            _finishScreenLiveData.value = Event(true)
        } else {
            _errorLiveData.value = Event(true)
        }
    }
}
