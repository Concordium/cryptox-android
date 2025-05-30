package com.concordium.wallet.ui.seed.recover.googledrive.password

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.data.export.EncryptedExportData
import com.concordium.wallet.databinding.ActivityRecoverGoogleDrivePasswordBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.seed.recoverprocess.RecoverProcessActivity
import com.concordium.wallet.util.getSerializable

class RecoverGoogleDrivePasswordActivity : BaseActivity(
    R.layout.activity_recover_google_drive_password,
    R.string.import_google_drive_enter_encryption_password_title
), AuthDelegate by AuthDelegateImpl() {

    private lateinit var exportData: EncryptedExportData

    private val binding by lazy {
        ActivityRecoverGoogleDrivePasswordBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: RecoverGoogleDrivePasswordViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exportData = requireNotNull(
            intent.getSerializable(ENCRYPTED_DATA, EncryptedExportData::class.java)
        )

        hideActionBarBack(isVisible = true)
        initViews()
        initObservers()
    }

    private fun initViews() {
        binding.enterPasswordInputLayout.apply {
            setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            setOnSearchDoneListener { onEnterPasswordConfirm() }
            setAfterTextChangedListener {
                enterPasswordButtonEnabled()
                showPasswordError(false)
                binding.enterPasswordError.isVisible = false
            }
        }
        binding.enterPasswordButton.setOnClickListener {
            onEnterPasswordConfirm()
        }
    }

    private fun initObservers() {
        viewModel.saveSeedPhrase.collectWhenStarted(this) { saveSuccess ->
            goToRecovery(saveSuccess)
        }
        viewModel.loading.collectWhenStarted(this) { loading ->
            showLoading(loading)
        }
        viewModel.showEnterPasswordError.collectWhenStarted(this) { show ->
            showPasswordError(show)
        }
        viewModel.showAuthentication.collectWhenStarted(this) { show ->
            if (show) {
                showAuthentication(this@RecoverGoogleDrivePasswordActivity) { password ->
                    viewModel.setSeedPhrase(exportData, password)
                }
            }
        }
        viewModel.error.collectWhenStarted(this) { error ->
            error.contentIfNotHandled?.takeIf { it != -1 }?.let { resId ->
                binding.enterPasswordError.run {
                    text = getString(resId)
                    isVisible = true
                }
            }
        }
    }

    private fun onEnterPasswordConfirm() {
        viewModel.onEnterPasswordConfirmClicked(binding.enterPasswordInputLayout.getText())
    }

    private fun enterPasswordButtonEnabled() {
        binding.apply {
            enterPasswordButton.isEnabled = enterPasswordInputLayout.getText().isNotEmpty()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loading.progressBar.isVisible = show
    }

    private fun showPasswordError(show: Boolean) {
        binding.enterPasswordInputError.isVisible = show
    }

    private fun goToRecovery(success: Boolean) {
        if (success) {
            finish()
            startActivity(Intent(this, RecoverProcessActivity::class.java))
        } else {
            showError(R.string.auth_login_seed_error)
        }
    }

    companion object {
        const val ENCRYPTED_DATA = "encrypted_data"
    }
}