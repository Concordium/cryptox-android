package com.concordium.wallet.ui.seed.recover.googledrive.backupslist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.backup.GoogleDriveManager.listFilesInAppFolder
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

    fun getBackupsList(driveService: Drive) = viewModelScope.launch(Dispatchers.IO) {
        _loading.emit(true)
        _backupsList.emit(listFilesInAppFolder(driveService))
        _loading.emit(false)
    }

    fun downloadFileFromAppFolder(
        driveService: Drive,
        fileName: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        _loading.emit(true)

        val fileList = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='$fileName' and trashed=false and 'appDataFolder' in parents")
            .setFields("files(id, name, createdTime)")
            .execute()

        val file = fileList.files.firstOrNull() ?: run {
            println("File not found in app folder: $fileName")
            return@launch
        }

        val outputStream = ByteArrayOutputStream()
        driveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
        Log.d("Downloaded file '${file.name}' (${outputStream.size()} bytes)")
        Log.d("Created time: ${file.createdTime}")

        val result = outputStream.toByteArray()
        Log.d(
            "Downloaded content: ${
                result.inputStream().bufferedReader().readText().prettyPrint()
            }"
        )
        val jsonString = result.toString(Charsets.UTF_8)
        val encryptedData = App.appCore.gson.fromJson(jsonString, EncryptedExportData::class.java)
        Log.d("Encrypted content: $encryptedData")
        _encryptedData.emit(encryptedData)
        _loading.emit(false)
    }

    fun setHasGoogleAccountSignedIn(value: Boolean) {
        App.appCore.setup.setGoogleAccountSignedIn(value)
    }
}