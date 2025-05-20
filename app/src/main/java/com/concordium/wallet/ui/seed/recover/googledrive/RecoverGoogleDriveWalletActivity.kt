package com.concordium.wallet.ui.seed.recover.googledrive

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityRecoverGoogleDriveWalletBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.seed.recoverprocess.RecoverProcessActivity
import com.concordium.wallet.ui.seed.reveal.GoogleDriveManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.launch

class RecoverGoogleDriveWalletActivity :
    BaseActivity(R.layout.activity_recover_google_drive_wallet),
    AuthDelegate by AuthDelegateImpl() {

    private val binding by lazy {
        ActivityRecoverGoogleDriveWalletBinding.bind(findViewById(R.id.root_layout))
    }

    val viewModel: GoogleDriveRecoverViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        if (task.isSuccessful) {
            val account = task.result
            lifecycleScope.launch {

                // Create Drive service for listing/downloading
                val driveService = GoogleDriveManager.getDriveService(
                    this@RecoverGoogleDriveWalletActivity,
                    account
                )
                viewModel.getBackupsList(driveService)
                viewModel.downloadFileFromAppFolder(driveService)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        setActionBarTitle("")

        setupGoogleSignIn()
        initObservers()
    }

    private fun initObservers() {
        viewModel.saveSeedPhrase.collectWhenStarted(this) { saveSuccess ->
            goToRecovery(saveSuccess)
        }

        viewModel.loading.collectWhenStarted(this) { loading ->
            binding.loading.progressBar.isVisible = loading
        }

        viewModel.encryptedData.collectWhenStarted(this) { data ->
            data?.let {
                showAuthentication(this) { password ->
                    viewModel.setSeedPhrase(data, password)
                }
            }
        }
    }

    private fun setupGoogleSignIn() {
        signInWithGoogle(GoogleDriveManager.getSignInClient(this))
    }

    private fun signInWithGoogle(googleSignInClient: GoogleSignInClient) {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun goToRecovery(success: Boolean) {
        if (success) {
            finish()
            startActivity(Intent(this, RecoverProcessActivity::class.java))
        } else {
            showError(R.string.auth_login_seed_error)
        }
    }
}