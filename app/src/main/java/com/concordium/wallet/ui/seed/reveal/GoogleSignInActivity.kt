package com.concordium.wallet.ui.seed.reveal

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GoogleSignInActivity : BaseActivity(R.layout.activity_google_drive_backup) {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var accessToken: String

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        if (task.isSuccessful) {
            val account = task.result
            lifecycleScope.launch {
                accessToken = getAccessToken(account)

                // Upload file to appDataFolder
                uploadToDrive("Hello Google Drive!".toByteArray(), "cryptox_backup.txt")

                // Create Drive service for listing/downloading
                val driveService = createDriveService(account)

                // List files
                val files = listFilesInAppFolder(driveService)

                // Download a file if it exists
                val downloadedBytes = downloadFileFromAppFolder(driveService, "cryptox_backup.txt")
                downloadedBytes?.let {
                    println("Downloaded content: ${it.inputStream().bufferedReader().readText()}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        setupGoogleSignIn()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .requestIdToken("124880082147-7ssi8hf915qdbd3i4n02d9kaa8l2au0r.apps.googleusercontent.com")
            .requestServerAuthCode("124880082147-7ssi8hf915qdbd3i4n02d9kaa8l2au0r.apps.googleusercontent.com")
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signIn()
    }

    private suspend fun getAccessToken(account: GoogleSignInAccount?): String =
        withContext(Dispatchers.IO) {
            GoogleAuthUtil.getToken(
                this@GoogleSignInActivity,
                account!!.account!!,
                "oauth2:${DriveScopes.DRIVE_FILE}"
            )
        }

    private fun createDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(getString(R.string.app_name)).build()
    }


    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private suspend fun uploadToDrive(data: ByteArray, filename: String) =
        withContext(Dispatchers.IO) {
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

            val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            connection.doOutput = true
            connection.outputStream.write(bodyStream.toByteArray())

            val responseCode = connection.responseCode
            println("Upload response code: $responseCode")
            val response = connection.inputStream.bufferedReader().readText()
            println("Upload response: $response")

            connection.disconnect()
        }

    private suspend fun listFilesInAppFolder(driveService: Drive): List<File> =
        withContext(Dispatchers.IO) {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("'appDataFolder' in parents and trashed=false")
                .setFields("files(id, name)")
                .execute()

            result.files.onEach { file ->
                println("Found file: ${file.name} (ID: ${file.id})")
            }
        }

    private suspend fun downloadFileFromAppFolder(
        driveService: Drive,
        fileName: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        val fileList = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='$fileName' and trashed=false and 'appDataFolder' in parents")
            .setFields("files(id, name)")
            .execute()

        val file = fileList.files.firstOrNull() ?: run {
            println("File not found in app folder: $fileName")
            return@withContext null
        }

        val outputStream = ByteArrayOutputStream()
        driveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
        println("Downloaded file '${file.name}' (${outputStream.size()} bytes)")

        return@withContext outputStream.toByteArray()
    }
}