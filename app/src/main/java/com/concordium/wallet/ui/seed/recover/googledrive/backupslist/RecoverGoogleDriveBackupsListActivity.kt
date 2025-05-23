package com.concordium.wallet.ui.seed.recover.googledrive.backupslist

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.core.backup.GoogleDriveManager
import com.concordium.wallet.data.export.EncryptedExportData
import com.concordium.wallet.databinding.ActivityRecoverGoogleDriveBackupsListBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.seed.recover.googledrive.password.RecoverGoogleDrivePasswordActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File

class RecoverGoogleDriveBackupsListActivity :
    BaseActivity(
        R.layout.activity_recover_google_drive_backups_list,
        R.string.welcome_recover_google_drive_select_backup_title
    ), AuthDelegate by AuthDelegateImpl() {

    private lateinit var backupsAdapter: RecoverGoogleDriveBackupsListAdapter
    private lateinit var driveService: Drive

    private val binding by lazy {
        ActivityRecoverGoogleDriveBackupsListBinding.bind(findViewById(R.id.root_layout))
    }

    val viewModel: RecoverGoogleDriveBackupsListViewModel by lazy {
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

            driveService = GoogleDriveManager.getDriveService(
                this@RecoverGoogleDriveBackupsListActivity,
                account
            )
            viewModel.getBackupsList(driveService)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActionBarBack(isVisible = true)

        setupGoogleSignIn()
        initViews()
        initObservers()
    }

    private fun initViews() {
        backupsAdapter = RecoverGoogleDriveBackupsListAdapter()
        binding.backupsList.adapter = backupsAdapter
        backupsAdapter.setBackupClickListener(object :
            RecoverGoogleDriveBackupsListAdapter.BackupClickListener {
            override fun onBackupClick(file: File) {
                viewModel.downloadFileFromAppFolder(driveService, file.name)
            }
        })
    }

    private fun initObservers() {
        viewModel.loading.collectWhenStarted(this) { loading ->
            binding.loading.progressBar.isVisible = loading
        }

        viewModel.encryptedData.collectWhenStarted(this) { data ->
            data?.let {
                goToEnterPassword(data)
            }
        }

        viewModel.backupsList.collectWhenStarted(this) { backupsList ->
            binding.backupsList.isVisible = backupsList.isNotEmpty()
            backupsAdapter.setFiles(backupsList)
        }
    }

    private fun setupGoogleSignIn() {
        signInWithGoogle(GoogleDriveManager.getSignInClient(this))
    }

    private fun signInWithGoogle(googleSignInClient: GoogleSignInClient) {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun goToEnterPassword(data: EncryptedExportData) {
        val intent = Intent(this, RecoverGoogleDrivePasswordActivity::class.java)
        intent.putExtra(RecoverGoogleDrivePasswordActivity.ENCRYPTED_DATA, data)
        startActivity(intent)
    }
}