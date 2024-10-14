package com.concordium.wallet.ui.seed.recover.seed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.data.preferences.WalletSetupPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.web3j.utils.Numeric.hexStringToByteArray

class SeedRecoverViewModel(application: Application) : AndroidViewModel(application) {

    private val session: Session = App.appCore.session

    private val _saveSeedSuccess = MutableStateFlow(false)
    val saveSeedSuccess = _saveSeedSuccess.asStateFlow()

    private val _validate = MutableStateFlow(false)
    val validate = _validate.asStateFlow()

    var seed = MutableStateFlow("")

    fun setPredefinedSeedForTesting(password: String) = viewModelScope.launch {
        if (BuildConfig.DEBUG) {
            setSeed(
                "575f0f919c99ed4b7d858df2aea68112292da4eae98e2e69410cd5283f3c727b282caeba754f815dc876d8b84d3339c6f74c4127f238a391891dd23c74892943",
                password
            )
        }
    }

    fun validateSeed() {
        val hexPattern = Regex("^[0-9a-fA-F]{128}$")
        val success = if (hexPattern.matches(seed.value)) {
            try {
                val decodedBytes = hexStringToByteArray(seed.value)
                decodedBytes.size == 64
            } catch (e: IllegalArgumentException) {
                false
            }
        } else {
            false
        }

        _validate.value = success
    }

    fun setSeed(privateKey: String, password: String) = viewModelScope.launch {
        val success = WalletSetupPreferences(getApplication()).tryToSetEncryptedSeedHex(
            privateKey,
            password
        )
        if (success) {
            session.hasCompletedInitialSetup()
        }
        _saveSeedSuccess.value = success
    }

    fun clearSeed() {
        seed.value = ""
    }
}
