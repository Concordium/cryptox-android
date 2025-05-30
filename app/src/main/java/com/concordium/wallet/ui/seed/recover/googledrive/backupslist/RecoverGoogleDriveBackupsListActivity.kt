package com.concordium.wallet.ui.seed.recover.googledrive.backupslist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.core.backup.GoogleDriveManager
import com.concordium.wallet.data.export.EncryptedExportData
import com.concordium.wallet.databinding.ActivityRecoverGoogleDriveBackupsListBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.GoogleSignInDelegate
import com.concordium.wallet.ui.common.delegates.GoogleSignInDelegateImpl
import com.concordium.wallet.ui.seed.recover.googledrive.password.RecoverGoogleDrivePasswordActivity
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File

class RecoverGoogleDriveBackupsListActivity :
    BaseActivity(
        R.layout.activity_recover_google_drive_backups_list,
        R.string.welcome_recover_google_drive_select_backup_title
    ), GoogleSignInDelegate by GoogleSignInDelegateImpl() {

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
                viewModel.downloadBackupFromAppFolder(driveService, file.name)
            }
        })
        listOf(
            binding.changeAccountButton,
            binding.emptyChangeAccountButton
        ).forEach {
            it.setOnClickListener {
                changeGoogleAccount()
            }
        }
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

        viewModel.state.collectWhenStarted(this) { state ->
            when (state) {
                RecoverGoogleDriveBackupsListViewModel.State.Processing -> {
                    setActionBarTitle(getString(R.string.settings_overview_google_drive_processing))
                    binding.chooseBackupMessage.isVisible = false
                    binding.backupsList.isVisible = false
                    binding.changeAccountButton.isVisible = false
                    binding.emptyStateLayout.isVisible = false
                    binding.loading.progressBar.isVisible = true
                }

                RecoverGoogleDriveBackupsListViewModel.State.Success -> {
                    setActionBarTitle(getString(R.string.welcome_recover_google_drive_select_backup_title))
                    binding.chooseBackupMessage.isVisible = true
                    binding.backupsList.isVisible = true
                    binding.changeAccountButton.isVisible = true
                    binding.emptyStateLayout.isVisible = false
                    binding.loading.progressBar.isVisible = false
                }

                RecoverGoogleDriveBackupsListViewModel.State.EmptyState -> {
                    setActionBarTitle("")
                    binding.chooseBackupMessage.isVisible = false
                    binding.backupsList.isVisible = false
                    binding.changeAccountButton.isVisible = false
                    binding.emptyStateLayout.isVisible = true
                    binding.loading.progressBar.isVisible = false
                }

                is RecoverGoogleDriveBackupsListViewModel.State.Error -> {
                    showErrorToast(state.message)
                    finish()
                }
            }
        }
    }

    private fun setupGoogleSignIn() {
        registerLauncher(
            caller = this,
            onSuccess = { account ->
                driveService = GoogleDriveManager.getDriveService(this, account)
                viewModel.getBackupsList(driveService)
                viewModel.setHasGoogleAccountSignedIn(true)
            },
            onFailure = {
                viewModel.setHasGoogleAccountSignedIn(false)
                showErrorToast(getString(R.string.settings_overview_google_drive_permissions_error))
                finish()
            }
        )
        val googleSignInClient = GoogleDriveManager.getSignInClient(this)
        val signInIntent = googleSignInClient.signInIntent
        launchGoogleSignIn(signInIntent)
    }

    private fun changeGoogleAccount() {
        val signInAccount = GoogleDriveManager.getSignInClient(this)
        signInAccount.signOut().addOnCompleteListener {
            if (it.isSuccessful) {
                viewModel.setHasGoogleAccountSignedIn(false)
                launchGoogleSignIn(signInAccount.signInIntent)
            } else {
                showError(R.string.app_error_lib)
            }
        }
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun goToEnterPassword(data: EncryptedExportData) {
        val intent = Intent(this, RecoverGoogleDrivePasswordActivity::class.java)
        intent.putExtra(RecoverGoogleDrivePasswordActivity.ENCRYPTED_DATA, data)
        startActivity(intent)
    }
}