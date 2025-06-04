package com.concordium.wallet.ui.seed.recover.googledrive.backupslist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
import com.concordium.wallet.core.backup.GoogleDriveManager
import com.concordium.wallet.data.export.EncryptedExportData
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PrettyPrint.prettyPrint
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class RecoverGoogleDriveBackupsListViewModel(application: Application) :
    AndroidViewModel(application) {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _encryptedData = MutableSharedFlow<EncryptedExportData?>()
    val encryptedData = _encryptedData.asSharedFlow()

    private val _backupsList = MutableStateFlow<List<File>>(mutableListOf())
    val backupsList = _backupsList.asStateFlow()

    private val _state = MutableStateFlow<State>(State.Processing)
    val state = _state.asStateFlow()

    /**
     * Returns the list of available backups.
     *
     * Note: This function depends on the backup naming behavior in
     * [com.concordium.wallet.ui.seed.reveal.backup.GoogleDriveCreateBackupViewModel.getBackupName]
     */
    fun getBackupsList(driveService: Drive) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.Processing)
        try {
            val list = GoogleDriveManager.listFilesInAppFolder(driveService)
                .filter {
                    it.name.contains(AppConfig.net)
                }
            _backupsList.emit(list)
            if (list.isEmpty()) {
                _state.emit(State.EmptyState)
            } else {
                _state.emit(State.Success)
            }
        } catch (e: Exception) {
            Log.d("Failed to download backup list, error: $e")
            _state.emit(State.Error(e.localizedMessage ?: "Failed to download backup list"))
        }
    }

    fun downloadBackupFromAppFolder(
        driveService: Drive,
        fileName: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        _loading.emit(true)
        try {
            val file = GoogleDriveManager.listFilesInAppFolder(driveService)
                .firstOrNull() ?: run {
                Log.d("File not found in app folder: $fileName")
                return@launch
            }
            val outputStream = ByteArrayOutputStream()
            driveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
            val result = outputStream.toByteArray()
            Log.d(
                "Downloaded content: ${
                    result.inputStream().bufferedReader().readText().prettyPrint()
                }"
            )
            val jsonString = result.toString(Charsets.UTF_8)
            val encryptedData =
                App.appCore.gson.fromJson(jsonString, EncryptedExportData::class.java)
            Log.d("Encrypted content: $encryptedData")
            _encryptedData.emit(encryptedData)
        } catch (e: Exception) {
            Log.d("Failed to download backup, error: $e")
            _state.emit(State.Error(e.localizedMessage ?: "Failed to download backup"))
        } finally {
            _loading.emit(false)
        }
    }

    fun setHasGoogleAccountSignedIn(value: Boolean) {
        App.appCore.setup.setGoogleAccountSignedIn(value)
    }

    sealed interface State {
        object Processing : State
        object EmptyState : State
        object Success : State
        data class Error(val message: String) : State
    }
}