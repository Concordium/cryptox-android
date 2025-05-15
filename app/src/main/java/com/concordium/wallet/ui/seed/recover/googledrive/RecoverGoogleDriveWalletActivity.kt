package com.concordium.wallet.ui.seed.recover.googledrive

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.databinding.ActivityRecoverGoogleDriveWalletBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.PrettyPrint.prettyPrint
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

class RecoverGoogleDriveWalletActivity :
    BaseActivity(R.layout.activity_recover_google_drive_wallet),
    AuthDelegate by AuthDelegateImpl() {

    private val binding by lazy {
        ActivityRecoverGoogleDriveWalletBinding.bind(findViewById(R.id.root_layout))
    }

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

                // Create Drive service for listing/downloading
                val driveService = createDriveService(account)

                // List files
                val files = listFilesInAppFolder(driveService)

                // Download a file if it exists
                val downloadedBytes = downloadFileFromAppFolder(driveService, "cryptox_backup.txt")
                downloadedBytes?.let {
                    println("Downloaded content: ${it.inputStream().bufferedReader().readText().prettyPrint()}")
                    val jsonString = it.toString(Charsets.UTF_8)
                    val encryptedData = App.appCore.gson.fromJson(jsonString, EncryptedData::class.java)
                    println("Encrypted content: $encryptedData")

                    showAuthentication(this@RecoverGoogleDriveWalletActivity) { password ->
                        lifecycleScope.launch {
                            val seedPhrase =
                                App.appCore.session.walletStorage.setupPreferences.getSeedPhrase(
                                    password = password,
                                    encryptedData = encryptedData
                                )
                            println("Seed phrase: $seedPhrase")
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        setActionBarTitle("")

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
                this@RecoverGoogleDriveWalletActivity,
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
            .setFields("files(id, name, createdTime)")
            .execute()

        val file = fileList.files.firstOrNull() ?: run {
            println("File not found in app folder: $fileName")
            return@withContext null
        }

        val outputStream = ByteArrayOutputStream()
        driveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
        println("Downloaded file '${file.name}' (${outputStream.size()} bytes)")
        println("Created time: ${file.createdTime}")

        return@withContext outputStream.toByteArray()
    }
}