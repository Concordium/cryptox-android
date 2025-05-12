package com.concordium.wallet.ui.seed.reveal
//
//import android.content.Context
//import androidx.credentials.Credential
//import androidx.credentials.CredentialManager
//import androidx.credentials.CustomCredential
//import androidx.credentials.GetCredentialRequest
//import androidx.credentials.GetCredentialResponse
//import androidx.credentials.exceptions.NoCredentialException
//
//object GoogleSignInManager {
//
//    private lateinit var credentialManager: CredentialManager
//
//    private suspend fun googleSignIn(
//        context: Context,
//        apiKey: String,
//        filterByAuthorizedAccounts: Boolean,
//        doOnSuccess: (String) -> Unit,
//        doOnError: (Exception) -> Unit,
//    ) {
//        if (::credentialManager.isInitialized.not()) {
//            credentialManager = CredentialManager
//                .create(context)
//        }
//
//        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption
//            .Builder()
//            .setFilterByAuthorizedAccounts(true)
//            .setServerClientId(apiKey)
//            .setAutoSelectEnabled(false)
//            .build()
//
//        val request: GetCredentialRequest = GetCredentialRequest
//            .Builder()
//            .addCredentialOption(googleIdOption)
//            .build()
//
//        requestSignIn(
//            context,
//            request,
//            apiKey,
//            filterByAuthorizedAccounts,
//            doOnSuccess,
//            doOnError
//        )
//    }
//
//    private suspend fun requestSignIn(
//        context: Context,
//        request: GetCredentialRequest,
//        apiKey: String,
//        filterByAuthorizedAccounts: Boolean,
//        doOnSuccess: (String) -> Unit,
//        doOnError: (Exception) -> Unit,
//    ) {
//        try {
//            val result: GetCredentialResponse = credentialManager.getCredential(
//                request = request,
//                context = context,
//            )
//            val displayName = handleCredentials(result.credential)
//            displayName?.let {
//                doOnSuccess(displayName)
//            } ?: doOnError(Exception("Invalid user"))
//        } catch (e: Exception){
//            if (e is NoCredentialException && filterByAuthorizedAccounts) {
//                googleSignIn(
//                    context,
//                    apiKey,
//                    false,
//                    doOnSuccess,
//                    doOnError
//                )
//            } else {
//                doOnError(e)
//            }
//        }
//    }
//
//    private fun handleCredentials(credential: Credential): String? {
//        when (credential) {
//
//            // GoogleIdToken credential
//            is CustomCredential -> {
//                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
//                    try {
//                        // Use googleIdTokenCredential and extract id to validate and
//                        // authenticate on your server.
//                        val googleIdTokenCredential = GoogleIdTokenCredential
//                            .createFrom(credential.data)
//                        return googleIdTokenCredential.displayName
//                    } catch (e: GoogleIdTokenParsingException) {
//                        println("Received an invalid google id token response $e")
//                    }
//                } else {
//                    // Catch any unrecognized custom credential type here.
//                    println("Unexpected type of credential")
//                }
//            }
//
//            else -> {
//                // Catch any unrecognized credential type here.
//                println("Unexpected type of credential")
//            }
//        }
//        return null
//    }
//}