package com.concordium.wallet.ui.seed.reveal.backup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
import com.concordium.wallet.core.backup.GoogleDriveManager
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.ExportEncryptionHelper
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PrettyPrint.asJsonString
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GoogleDriveCreateBackupViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepository = AccountRepository(
        App.appCore.session.walletStorage.database.accountDao()
    )

    private val _showSetPasswordError = MutableSharedFlow<Boolean>(1)
    val showSetPasswordError = _showSetPasswordError.asSharedFlow()

    private val _showRepeatPasswordError = MutableSharedFlow<Boolean>(1)
    val showRepeatPasswordError = _showRepeatPasswordError.asSharedFlow()

    private val _showAuthentication = MutableSharedFlow<Boolean>(1)
    val showAuthentication = _showAuthentication.asSharedFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _state = MutableStateFlow<State>(State.Processing)
    val state = _state.asStateFlow()

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Processing)
    val backupStatus = _backupStatus.asStateFlow()

    private val _backupReady = MutableStateFlow(false)
    val backupReady = _backupReady.asStateFlow()

    private val _canCheckBackupStatus = MutableStateFlow(false)
    val canCheckBackupStatus = _canCheckBackupStatus.asStateFlow()

    private val _backupFile = MutableSharedFlow<File?>()
    val backupFile = _backupFile.asSharedFlow()

    private val backupPassword = MutableStateFlow("")
    private val googleAccessToken = MutableStateFlow("")
    private val googleSignInAccount = MutableStateFlow<GoogleSignInAccount?>(null)

    private fun isGoogleAccountSignedIn() =
        App.appCore.setup.isGoogleAccountSignedIn

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
            ExportEncryptionHelper.encryptExportData(backupPassword.value, seedPhrase)
        } catch (e: Exception) {
            Log.e("phrase_encrypt_failed", e)
            _loading.emit(false)
            return@launch
        }

        uploadToDrive(
            encryptedExportData.asJsonString().toByteArray(),
            getBackupName()
        )
    }

    fun backToSetPassword() = viewModelScope.launch {
        _state.emit(State.SetPassword)
    }

    fun setGoogleSignInAccount(account: GoogleSignInAccount) {
        isBackupExists(account)
    }

    fun checkBackupStatus() = viewModelScope.launch {
        _backupStatus.emit(BackupStatus.Processing)
        if (isGoogleAccountSignedIn().not()) {
            _backupStatus.emit(BackupStatus.NotBackedUp)
            return@launch
        } else {
            _canCheckBackupStatus.emit(true)
        }
    }

    private fun isBackupExists(account: GoogleSignInAccount) =
        viewModelScope.launch(Dispatchers.IO) {
            googleSignInAccount.emit(account)
            try {
                val token = GoogleDriveManager.getAccessToken(getApplication(), account)
                googleAccessToken.emit(token)
                val driveService = GoogleDriveManager.getDriveService(getApplication(), account)
                val backupsList = GoogleDriveManager.listFilesInAppFolder(driveService)
                val file = backupsList.find { it.name == getBackupName() }
                    ?: run {
                        _state.emit(State.SetPassword)
                        _backupStatus.emit(BackupStatus.NotBackedUp)
                        Log.d("Backup file not found")
                        return@launch
                    }
                file.let {
                    Log.d("Backup file found")
                    _backupFile.emit(file)
                    _state.emit(State.BackupAlreadyExists)
                    _backupStatus.emit(BackupStatus.BackedUp)
                }
            } catch (e: Exception) {
                Log.e("Google Drive sign in failed", e)
            }
        }

    fun deleteBackup() = viewModelScope.launch(Dispatchers.IO) {
        try {
            googleSignInAccount.first()?.let {
                _backupStatus.emit(BackupStatus.Processing)
                val driveService = GoogleDriveManager.getDriveService(getApplication(), it)
                val backupsList = GoogleDriveManager.listFilesInAppFolder(driveService)
                val fileName = getBackupName()

                backupsList.find { backup ->
                    backup.name == getBackupName()
                }?.let { file ->
                    driveService
                        .files()
                        .delete(file.id)
                        .execute()
                    Log.d("File ${file.name} deleted successfully.")
                } ?: run {
                    Log.d("File $fileName not found.")
                }
                setGoogleSignInAccount(it)
            }
        } catch (e: Exception) {
            Log.e("Failed to delete file", e)
        }
    }

    /**
     * Returns the name of the backup file.
     *
     * Any changes in backup name should be reflected in
     * [com.concordium.wallet.ui.seed.recover.googledrive.backupslist.RecoverGoogleDriveBackupsListViewModel.getBackupsList]
     */
    private suspend fun getBackupName(): String =
        "${AppConfig.net} backup ${
            Account.getDefaultName(
                accountRepository.getAllDone().first().address
            )
        }"

    fun setHasGoogleAccountSignedIn(value: Boolean) {
        App.appCore.setup.setGoogleAccountSignedIn(value)
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
                Log.d("Upload response code: $responseCode")
                val response = connection.inputStream.bufferedReader().readText()
                Log.d("Upload response: $response")
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
        object BackupAlreadyExists : State
    }

    sealed interface BackupStatus {
        object Processing : BackupStatus
        object NotBackedUp : BackupStatus
        object BackedUp : BackupStatus
    }
}