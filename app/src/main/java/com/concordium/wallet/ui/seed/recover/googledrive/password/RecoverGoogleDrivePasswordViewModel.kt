package com.concordium.wallet.ui.seed.recover.googledrive.password

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.core.multiwallet.SwitchActiveWalletTypeUseCase
import com.concordium.wallet.core.security.EncryptionException
import com.concordium.wallet.data.export.EncryptedExportData
import com.concordium.wallet.data.util.ExportEncryptionHelper
import com.concordium.wallet.util.Log
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecoverGoogleDrivePasswordViewModel(application: Application) :
    AndroidViewModel(application) {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _saveSeedPhrase = MutableStateFlow(false)
    val saveSeedPhrase = _saveSeedPhrase.asStateFlow()

    private val _showEnterPasswordError = MutableStateFlow(false)
    val showEnterPasswordError = _showEnterPasswordError.asStateFlow()

    private val _showAuthentication = MutableSharedFlow<Boolean>(1)
    val showAuthentication = _showAuthentication.asSharedFlow()

    private val _error = MutableStateFlow(Event(-1))
    val error = _error.asStateFlow()

    private val backupPassword = MutableStateFlow("")

    fun onEnterPasswordConfirmClicked(password: String) {
        viewModelScope.launch {
            if (password.length < 6) {
                _showEnterPasswordError.emit(true)
            } else {
                _showEnterPasswordError.emit(false)
                backupPassword.emit(password)
                _showAuthentication.emit(true)
            }
        }
    }

    fun setSeedPhrase(encryptedData: EncryptedExportData, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val seedPhrase = try {
                ExportEncryptionHelper.decryptExportData(backupPassword.value, encryptedData)
            } catch (e: IllegalArgumentException) {
                Log.e("Unexpected file format")
                _error.emit(Event(R.string.import_google_drive_enter_encryption_password_error_file_format))
                return@launch
            } catch (e: Exception) {
                when (e) {
                    is JsonIOException,
                    is JsonSyntaxException,
                    -> {
                        Log.e("Unexpected file format")
                        _error.emit(Event(R.string.import_google_drive_enter_encryption_password_error_file_format))
                    }

                    is EncryptionException -> {
                        Log.e("Unexpected encryption/decryption error")
                        _error.emit(Event(R.string.import_error_password_or_file_content))
                    }

                    else -> {
                        Log.e("Unexpected error")
                        _error.emit(Event(R.string.auth_login_seed_error))
                    }
                }
                return@launch
            }

            try {
                val isSavedSuccessfully = App.appCore.session.walletStorage.setupPreferences
                    .tryToSetEncryptedSeedPhrase(seedPhrase, password)

                if (isSavedSuccessfully) {
                    SwitchActiveWalletTypeUseCase().invoke(newWalletType = AppWallet.Type.SEED)
                    App.appCore.setup.finishInitialSetup()
                    App.appCore.session.walletStorage.setupPreferences.setHasCompletedOnboarding(
                        true
                    )
                }
                _saveSeedPhrase.emit(isSavedSuccessfully)
            } catch (e: Exception) {
                Log.e("Unexpected encryption error")
                _error.emit(Event(R.string.auth_login_seed_error))
            }
        }
    }
}