package com.concordium.wallet.ui.seed.recover.private_key

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.data.preferences.AuthPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrivateKeyRecoverViewModel(application: Application) : AndroidViewModel(application) {

    private val session: Session = App.appCore.session

    private val _saveKeySuccess = MutableStateFlow(false)
    val saveKeySuccess = _saveKeySuccess.asStateFlow()

    private val _validate = MutableStateFlow(false)
    val validate = _validate.asStateFlow()

    var privateKey = MutableStateFlow("")

    fun setPredefinedKeyForTesting(password: String) = viewModelScope.launch {
        if (BuildConfig.DEBUG) {
            setPrivateKey(
                "575f0f919c99ed4b7d858df2aea68112292da4eae98e2e69410cd5283f3c727b282caeba754f815dc876d8b84d3339c6f74c4127f238a391891dd23c74892943",
                password
            )
        }
    }

    fun validatePrivateKey() {
        var success = false
        if (privateKey.value.length < PRIVATE_KEY_LENGTH) {
            success = false
        }
        if (privateKey.value.length == PRIVATE_KEY_LENGTH) {
            success = privateKey.value.contains(" ").not()
        }
        _validate.value = success
    }

    fun setPrivateKey(privateKey: String, password: String) = viewModelScope.launch {
        val success = AuthPreferences(getApplication()).tryToSetEncryptedSeedHex(
            privateKey,
            password
        )
        if (success) {
            session.hasCompletedInitialSetup()
        }
        _saveKeySuccess.value = success
    }

    fun clearPrivateKey() {
        privateKey.value = ""
    }

    companion object {
        const val PRIVATE_KEY_LENGTH = 128
    }

}