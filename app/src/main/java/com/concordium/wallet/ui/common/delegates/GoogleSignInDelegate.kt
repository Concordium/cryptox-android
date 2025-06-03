package com.concordium.wallet.ui.common.delegates

import android.content.Intent
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

interface GoogleSignInDelegate {
    fun launchGoogleSignIn(intent: Intent)
    fun registerLauncher(
        caller: ActivityResultCaller,
        onSuccess: (GoogleSignInAccount) -> Unit,
        onFailure: (Throwable) -> Unit
    )
}

class GoogleSignInDelegateImpl : GoogleSignInDelegate {
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun registerLauncher(
        caller: ActivityResultCaller,
        onSuccess: (GoogleSignInAccount) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        googleSignInLauncher = caller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                task.result?.let { onSuccess(it) } ?: onFailure(Throwable(task.exception?.cause))
            } else {
                onFailure(Throwable(task.exception?.cause))
            }
        }
    }

    override fun launchGoogleSignIn(intent: Intent) {
        googleSignInLauncher.launch(intent)
    }
}