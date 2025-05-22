package com.concordium.wallet.core.backup

import android.content.Context
import com.concordium.wallet.R
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
import kotlinx.coroutines.withContext

object GoogleDriveManager {

    suspend fun getAccessToken(context: Context, account: GoogleSignInAccount?): String =
        withContext(Dispatchers.IO) {
            GoogleAuthUtil.getToken(
                context,
                account!!.account!!,
                "oauth2:${DriveScopes.DRIVE_FILE}"
            )
        }

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .requestIdToken("124880082147-7ssi8hf915qdbd3i4n02d9kaa8l2au0r.apps.googleusercontent.com")
            .requestServerAuthCode("124880082147-7ssi8hf915qdbd3i4n02d9kaa8l2au0r.apps.googleusercontent.com")
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun listFilesInAppFolder(driveService: Drive): List<File> =
        withContext(Dispatchers.IO) {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("'appDataFolder' in parents and trashed=false")
                .setFields("files(id, name, createdTime)")
                .execute()

            result.files.onEach { file ->
                println("Found file: ${file.name} (ID: ${file.id})")
            }
        }

    fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(context.getString(R.string.app_name)).build()
    }
}