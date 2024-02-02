package com.concordium.wallet.ui.auth.setuppassword

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAuthSetupPasswordBinding
import com.concordium.wallet.ui.auth.setupbiometrics.AuthSetupBiometricsActivity
import com.concordium.wallet.ui.auth.setuppasswordrepeat.AuthSetupPasswordRepeatActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.util.KeyboardUtil

class AuthSetupPasswordActivity : BaseActivity(
    R.layout.activity_auth_setup_password,
    R.string.auth_setup_password_title
) {
    private lateinit var viewModel: AuthSetupPasswordViewModel
    private val binding by lazy {
        ActivityAuthSetupPasswordBinding.bind(findViewById(R.id.root_layout))
    }

    private val skipBiometrics: Boolean by lazy {
        intent.getBooleanExtra(SKIP_BIOMETRICS, false)
    }

    companion object {
        private const val REQUEST_CODE_AUTH_SETUP_BIOMETRICS = 2000
        private const val REQUEST_CODE_AUTH_SETUP_PASSWORD_REPEAT = 2001
        const val SKIP_BIOMETRICS = "skip_biometrics"
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize(skipBiometrics)
        initializeViews()

        hideActionBarBack(isVisible = true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_AUTH_SETUP_BIOMETRICS) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        if (requestCode == REQUEST_CODE_AUTH_SETUP_PASSWORD_REPEAT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (AuthSetupPasswordRepeatActivity.usePasscode(data)) {
                    finish()
                } else if (AuthSetupPasswordRepeatActivity.doesMatch(data)) {
                    viewModel.setupPassword(binding.passwordEdittext.text.toString())
                } else {
                    binding.passwordEdittext.setText("")
                    binding.errorTextview.isVisible = true
                    binding.errorTextview.setText(R.string.auth_error_passwords_different)
                }
            } else {
                finish()
            }
        }
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AuthSetupPasswordViewModel::class.java]

        viewModel.errorLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showPasswordError()
                }
            }
        })
        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        })
        viewModel.gotoBiometricsSetupLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    gotoAuthSetupBiometrics()
                }
            }
        })
    }

    private fun initializeViews() {
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
        }
        binding.confirmButton.isEnabled = false
        binding.passwordEdittext.afterTextChanged {
            binding.errorTextview.isVisible = false
            binding.errorTextview.text = ""

            binding.confirmButton.isVisible = it.isNotEmpty()
            binding.confirmButton.isEnabled =
                viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())

            binding.passcodeButton.isVisible = it.isEmpty()
        }
        binding.passwordEdittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
                        onConfirmClicked()
                    }
                    true
                }

                else -> false
            }
        }
        binding.passcodeButton.setOnClickListener {
            finish()
        }
        binding.passwordEdittext.requestFocus()
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun onConfirmClicked() {
        if (viewModel.checkPasswordRequirements(binding.passwordEdittext.text.toString())) {
            viewModel.startSetupPassword(binding.passwordEdittext.text.toString())
            gotoAuthSetupPasswordRepeat()
        } else {
            binding.passwordEdittext.setText("")
            binding.errorTextview.isVisible = true
            binding.errorTextview.setText(R.string.auth_error_password_not_valid)
        }
    }

    private fun gotoAuthSetupBiometrics() {
        val intent = Intent(this, AuthSetupBiometricsActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_AUTH_SETUP_BIOMETRICS)
    }

    private fun gotoAuthSetupPasswordRepeat() {
        val intent = Intent(this, AuthSetupPasswordRepeatActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_AUTH_SETUP_PASSWORD_REPEAT)
    }

    private fun showPasswordError() {
        binding.passwordEdittext.setText("")
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.root, R.string.auth_error_password_setup)
    }

    //endregion

    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }
}
