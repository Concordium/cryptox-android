package com.concordium.wallet.ui.auth.setuppassword

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.util.BiometricsUtil
import kotlinx.coroutines.launch

class AuthSetupPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val session: Session = App.appCore.session

    private val _errorLiveData = MutableLiveData<Event<Boolean>>()
    val errorLiveData: LiveData<Event<Boolean>>
        get() = _errorLiveData
    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData
    private val _gotoBiometricsSetupLiveData = MutableLiveData<Event<Boolean>>()
    val gotoBiometricsSetupLiveData: LiveData<Event<Boolean>>
        get() = _gotoBiometricsSetupLiveData
    private var skipBiometrics = true

    fun initialize(skipBiometrics: Boolean) {
        this.skipBiometrics = skipBiometrics
    }

    fun startSetupPassword(password: String) {
        // Keep password for re-enter check and biometrics activation
        session.startPasswordSetup(password)
    }

    fun checkPasswordRequirements(password: String): Boolean {
        return (password.length >= 6)
    }

    fun setupPassword(password: String) = viewModelScope.launch {
        val isSetUpSuccessfully =
            runCatching {
                App.appCore.authManager.initPasswordAuth(password)
            }.isSuccess

        if (isSetUpSuccessfully) {
            // Setting up password is done, so login screen should be shown next time app is opened
            session.hasSetupPassword()
            if (BiometricsUtil.isBiometricsAvailable()) {
                if (skipBiometrics) {
                    _finishScreenLiveData.value = Event(true)
                } else {
                    _gotoBiometricsSetupLiveData.value = Event(true)
                }
            } else {
                session.hasFinishedSetupPassword()
                _finishScreenLiveData.value = Event(true)
            }
        } else {
            _errorLiveData.value = Event(true)
        }
    }
}
