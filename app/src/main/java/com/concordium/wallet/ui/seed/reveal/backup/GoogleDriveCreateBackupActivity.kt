package com.concordium.wallet.ui.seed.reveal.backup

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.core.backup.GoogleDriveManager
import com.concordium.wallet.databinding.ActivityCreateGoogleDriveBackupBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.delegates.GoogleSignInDelegate
import com.concordium.wallet.ui.common.delegates.GoogleSignInDelegateImpl
import com.concordium.wallet.uicore.toast.showGradientToast
import com.concordium.wallet.util.KeyboardUtil

class GoogleDriveCreateBackupActivity : BaseActivity(R.layout.activity_create_google_drive_backup),
    AuthDelegate by AuthDelegateImpl(), GoogleSignInDelegate by GoogleSignInDelegateImpl() {

    private val binding: ActivityCreateGoogleDriveBackupBinding by lazy {
        ActivityCreateGoogleDriveBackupBinding.bind(findViewById(R.id.root_layout))
    }

    private val viewModel: GoogleDriveCreateBackupViewModel by lazy {
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
        binding.setPasswordInputLayout.apply {
            setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            setOnSearchDoneListener { onSetPasswordConfirm() }
            setAfterTextChangedListener {
                showSetPasswordError(false)
                setPasswordButtonEnabled()
            }
        }
        binding.repeatPasswordInputLayout.apply {
            setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            setOnSearchDoneListener { onRepeatPasswordConfirm() }
            setAfterTextChangedListener {
                showRepeatPasswordError(false)
                setRepeatPasswordButtonEnabled()
            }
        }
        binding.setPasswordButton.setOnClickListener {
            onSetPasswordConfirm()
        }
        binding.repeatPasswordButton.setOnClickListener {
            onRepeatPasswordConfirm()
        }
    }

    private fun initObservers() {
        viewModel.loading.collectWhenStarted(this) { show ->
            showLoading(show)
        }
        viewModel.showSetPasswordError.collectWhenStarted(this) { show ->
            showSetPasswordError(show)
        }
        viewModel.showRepeatPasswordError.collectWhenStarted(this) { show ->
            showRepeatPasswordError(show)
        }
        viewModel.showAuthentication.collectWhenStarted(this) { show ->
            if (show) {
                showAuthentication(this) { password ->
                    viewModel.createBackup(password)
                }
            }
        }
        viewModel.backupReady.collectWhenStarted(this) { ready ->
            if (ready) {
                showGradientToast(
                    R.drawable.mw24_ic_address_copy_check,
                    getString(R.string.settings_overview_google_drive_backup_ready)
                )
                finish()
            }
        }
        viewModel.state.collectWhenStarted(this) { state ->
            when (state) {
                GoogleDriveCreateBackupViewModel.State.Processing -> {
                    showProcessingState()
                    hideActionBarBack(isVisible = true)
                }

                GoogleDriveCreateBackupViewModel.State.SetPassword -> {
                    showSetPasswordState()
                    hideActionBarBack(isVisible = true)
                }

                GoogleDriveCreateBackupViewModel.State.RepeatPassword -> {
                    showRepeatPasswordState()
                    hideActionBarBack(isVisible = true) {
                        viewModel.backToSetPassword()
                    }
                }

                GoogleDriveCreateBackupViewModel.State.BackupAlreadyExists -> {
                    Toast.makeText(
                        this,
                        getString(R.string.settings_overview_google_drive_backup_exists),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun setPasswordButtonEnabled() {
        binding.apply {
            setPasswordButton.isEnabled = setPasswordInputLayout.getText().isNotEmpty()
        }
    }

    private fun setRepeatPasswordButtonEnabled() {
        binding.apply {
            repeatPasswordButton.isEnabled = repeatPasswordInputLayout.getText().isNotEmpty()
        }
    }

    private fun showProcessingState() {
        setActionBarTitle(getString(R.string.settings_overview_google_drive_processing))
        binding.passwordRequirementsTextview.isVisible = false
        binding.setPasswordLayout.isVisible = false
        binding.repeatPasswordLayout.isVisible = false
        showLoading(true)
    }

    private fun showSetPasswordState() {
        setActionBarTitle(getString(R.string.settings_overview_google_drive_set_password_title))
        binding.passwordRequirementsTextview.isVisible = true
        binding.setPasswordLayout.isVisible = true
        binding.repeatPasswordLayout.isVisible = false
        showLoading(false)
        KeyboardUtil.showKeyboard(this, binding.setPasswordInputLayout)
    }

    private fun showRepeatPasswordState() {
        setActionBarTitle(getString(R.string.settings_overview_google_drive_repeat_password_title))
        binding.passwordRequirementsTextview.isVisible = true
        binding.setPasswordLayout.isVisible = false
        binding.repeatPasswordLayout.isVisible = true
        showLoading(false)
        KeyboardUtil.showKeyboard(this, binding.repeatPasswordInputLayout)
    }

    private fun onSetPasswordConfirm() {
        viewModel.onSetPasswordConfirmClicked(binding.setPasswordInputLayout.getText())
    }

    private fun showSetPasswordError(show: Boolean) {
        binding.setPasswordInputError.isVisible = show
    }

    private fun onRepeatPasswordConfirm() {
        viewModel.onRepeatPasswordConfirmClicked(binding.repeatPasswordInputLayout.getText())
    }

    private fun showRepeatPasswordError(show: Boolean) {
        binding.repeatPasswordInputError.isVisible = show
    }

    private fun showLoading(show: Boolean) {
        binding.loading.progressBar.isVisible = show
    }

    private fun setupGoogleSignIn() {
        registerLauncher(
            caller = this,
            onSuccess = { account ->
                viewModel.setHasGoogleAccountSignedIn(true)
                viewModel.setGoogleSignInAccount(account)
            },
            onFailure = { error ->
                Toast.makeText(
                    this,
                    error.message
                        ?: getString(R.string.settings_overview_google_drive_sign_in_failed),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        )
        val googleSignInClient = GoogleDriveManager.getSignInClient(this)
        val signInIntent = googleSignInClient.signInIntent
        launchGoogleSignIn(signInIntent)
    }
}