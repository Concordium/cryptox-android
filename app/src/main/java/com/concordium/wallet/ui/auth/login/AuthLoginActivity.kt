package com.concordium.wallet.ui.auth.login

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.databinding.ActivityAuthLoginBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.afterTextChanged
import com.concordium.wallet.uicore.view.PasscodeView
import com.concordium.wallet.util.KeyboardUtil
import javax.crypto.Cipher

class AuthLoginActivity : BaseActivity(
    R.layout.activity_auth_login,
    R.string.auth_login_title
) {
    private lateinit var viewModel: AuthLoginViewModel
    private val binding by lazy {
        ActivityAuthLoginBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var biometricPrompt: BiometricPrompt

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()

        if (viewModel.shouldShowBiometrics()) {
            showBiometrics()
        }

        hideActionBarBack(isVisible = false)
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AuthLoginViewModel::class.java]

        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    finish()
                }
            }
        })
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showErrorMessage(value)
            }
        })
        viewModel.passwordErrorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showPasswordError(value)
            }
        })
    }

    private fun initializeViews() {
        setActionBarTitle(if (viewModel.usePasscode()) R.string.auth_login_info_passcode else R.string.auth_login_info_password)
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
        }

        if (viewModel.usePasscode()) {
            binding.passwordEdittext.visibility = View.GONE
            binding.confirmButton.visibility = View.GONE
            binding.passcodeView.passcodeListener = object : PasscodeView.PasscodeListener {
                override fun onInputChanged() {
                    binding.errorTextview.isVisible = false
                    binding.errorTextview.text = ""
                }

                override fun onDone() {
                    onConfirmClicked()
                }
            }
            binding.passcodeView.requestFocus()
        } else {
            binding.passcodeView.visibility = View.GONE
            binding.passwordEdittext.setOnEditorActionListener { _, actionId, _ ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        onConfirmClicked()
                        true
                    }
                    else -> false
                }
            }
            binding.passwordEdittext.afterTextChanged {
                binding.confirmButton.isEnabled = it.isNotEmpty()
                binding.errorTextview.isVisible = false
                binding.errorTextview.text = ""
            }
            binding.passwordEdittext.requestFocus()
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun onConfirmClicked() {
        if (viewModel.usePasscode()) {
            viewModel.checkLogin(binding.passcodeView.getPasscode())
        } else {
            viewModel.checkLogin(binding.passwordEdittext.text.toString())
        }
    }

    private fun showBiometrics() {
        biometricPrompt = createBiometricPrompt()

        val promptInfo = createPromptInfo()

        val cipher = viewModel.getCipherForBiometrics()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun showPasswordError(stringRes: Int) {
        binding.passwordEdittext.setText("")
        binding.passcodeView.clearPasscode()
        binding.errorTextview.isVisible = true
        binding.errorTextview.setText(stringRes)
    }

    fun showErrorMessage(stringRes: Int) {
        binding.passwordEdittext.setText("")
        binding.passcodeView.clearPasscode()
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.rootLayout, stringRes)
    }

    //endregion

    //region Biometrics
    // ************************************************************

    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPromptCallback() {
            override fun onAuthenticationSucceeded(cipher: Cipher) {
                viewModel.checkLogin(cipher)
            }
        }

        return BiometricPrompt(this, executor, callback)
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login_biometrics_dialog_title))
            .setSubtitle(getString(R.string.auth_login_biometrics_dialog_subtitle))
            .setConfirmationRequired(false)
            .setNegativeButtonText(getString(if (viewModel.usePasscode()) R.string.auth_login_biometrics_dialog_cancel_passcode else R.string.auth_login_biometrics_dialog_cancel_password))
            .build()
    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }

    override fun loggedIn() {
        finish()
    }
    //endregion
}
