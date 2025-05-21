package com.concordium.wallet.ui.seed.reveal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.backup.GoogleDriveManager
import com.concordium.wallet.data.util.ExportEncryptionHelper
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PrettyPrint.asJsonString
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GoogleDriveSaveBackupViewModel(application: Application) : AndroidViewModel(application) {

    private val _showSetPasswordError = MutableSharedFlow<Boolean>(1)
    val showSetPasswordError = _showSetPasswordError.asSharedFlow()

    private val _showRepeatPasswordError = MutableSharedFlow<Boolean>(1)
    val showRepeatPasswordError = _showRepeatPasswordError.asSharedFlow()

    private val _showAuthentication = MutableStateFlow(false)
    val showAuthentication = _showAuthentication.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _state = MutableStateFlow<State>(State.Processing)
    val state = _state.asStateFlow()

    private val _backupReady = MutableStateFlow(false)
    val backupReady = _backupReady.asStateFlow()

    private val backupPassword = MutableStateFlow("")
    private val googleAccessToken = MutableStateFlow("")

    fun onSetPasswordConfirmClicked(password: String) {
        viewModelScope.launch {
            if (password.length < 6) {
                _showSetPasswordError.emit(true)
            } else {
                _showSetPasswordError.emit(false)
                backupPassword.emit(password)
                _state.emit(State.RepeatPassword)
            }
        }
    }

    fun onRepeatPasswordConfirmClicked(password: String) {
        viewModelScope.launch {
            if (backupPassword.value != password) {
                _showRepeatPasswordError.emit(true)
            } else {
                _showRepeatPasswordError.emit(false)
                _showAuthentication.emit(true)
            }
        }
    }

    fun createBackup(authPassword: String) = viewModelScope.launch(Dispatchers.IO) {
        _loading.emit(true)
        val seedPhrase = try {
            App.appCore.session.walletStorage.setupPreferences.getSeedPhrase(authPassword)
        } catch (e: Exception) {
            Log.e("phrase_decrypt_failed", e)
            _loading.emit(false)
            return@launch
        }

        val encryptedExportData = try {
            ExportEncryptionHelper.encryptExportData(authPassword, seedPhrase)
        } catch (e: Exception) {
            Log.e("phrase_encrypt_failed", e)
            _loading.emit(false)
            return@launch
        }

        uploadToDrive(
            encryptedExportData.asJsonString().toByteArray(),
            "cryptox_backup.txt"
        )
    }

    fun backToSetPassword() = viewModelScope.launch {
        _state.emit(State.SetPassword)
    }

    fun setGoogleSignInAccount(account: GoogleSignInAccount) {
        viewModelScope.launch(Dispatchers.IO) {
            googleAccessToken.emit(GoogleDriveManager.getAccessToken(getApplication(), account))
            _state.emit(State.SetPassword)
        }
    }

    private fun uploadToDrive(data: ByteArray, filename: String) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val metadata = """
        {
            "name": "$filename",
            "parents": ["appDataFolder"]
        }
    """.trimIndent()

                val boundary = "-------314159265358979323846"
                val delimiter = "--$boundary\r\n"
                val closeDelimiter = "--$boundary--"

                val bodyStream = ByteArrayOutputStream()
                val writer = OutputStreamWriter(bodyStream)
                writer.write(delimiter)
                writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                writer.write(metadata)
                writer.write("\r\n")

                writer.write(delimiter)
                writer.write("Content-Type: application/octet-stream\r\n\r\n")
                writer.flush()
                bodyStream.write(data)
                writer.write("\r\n$closeDelimiter")
                writer.flush()

                val url =
                    URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer ${googleAccessToken.value}")
                connection.setRequestProperty(
                    "Content-Type",
                    "multipart/related; boundary=$boundary"
                )
                connection.doOutput = true
                connection.outputStream.write(bodyStream.toByteArray())

                val responseCode = connection.responseCode
                println("Upload response code: $responseCode")
                val response = connection.inputStream.bufferedReader().readText()
                println("Upload response: $response")

                connection.disconnect()
                _loading.emit(false)
                _backupReady.emit(true)
            } catch (e: Exception) {
                Log.e("upload_failed", e)
                _loading.emit(false)
            }
        }

    sealed interface State {
        object Processing : State
        object SetPassword : State
        object RepeatPassword : State
    }
}