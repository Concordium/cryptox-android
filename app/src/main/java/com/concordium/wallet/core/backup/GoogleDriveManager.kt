@file:Suppress("DEPRECATION")

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

object GoogleDriveManager {

    fun getAccessToken(context: Context, account: GoogleSignInAccount?): String =
        GoogleAuthUtil.getToken(
            context,
            account!!.account!!,
            "oauth2:${DriveScopes.DRIVE_APPDATA}"
        )

    fun getSignInClient(context: Context): GoogleSignInClient {
        // No need to specify tokens – the client identification is done via Firebase.
        //
        //⚠️ Only packages signed by white-listed keys can do this.
        // Facing "Developer error 10" means your signing key SHA-1 is not white-listed
        // in the ConcordiumMobileWallet Firebase project.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    fun listFilesInAppFolder(driveService: Drive): List<File> =
        driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("'appDataFolder' in parents and trashed=false")
            .setFields("files(id, name, createdTime)")
            .execute()
            .files

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
