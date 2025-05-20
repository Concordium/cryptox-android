package com.concordium.wallet.ui.seed.reveal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.util.ExportEncryptionHelper
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleDriveSaveBackupViewModel(application: Application): AndroidViewModel(application) {

    fun encryptAndRevealPhrase(password: String) = viewModelScope.launch(Dispatchers.IO) {
        val seedPhrase = try {
            App.appCore.session.walletStorage.setupPreferences.getSeedPhrase(password)
        } catch (e: Exception) {
            Log.e("phrase_decrypt_failed", e)

            return@launch
        }

        Log.d("phrase_decrypted $seedPhrase")

        val encryptedExportData =
            ExportEncryptionHelper.encryptExportData(password, seedPhrase)

        Log.d("phrase_encrypted $encryptedExportData")

    }
}