package com.concordium.wallet.ui.auth.setupbiometrics

import android.app.Activity
import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.databinding.ActivityAuthSetupBiometricsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import javax.crypto.Cipher

class AuthSetupBiometricsActivity : BaseActivity(
    R.layout.activity_auth_setup_biometrics,
    R.string.auth_setup_biometrics_title
), AuthDelegate by AuthDelegateImpl() {
    private lateinit var viewModel: AuthSetupBiometricsViewModel
    private val binding by lazy {
        ActivityAuthSetupBiometricsBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var biometricPrompt: BiometricPrompt

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()

        hideActionBarBack(isVisible = false)

        biometricPrompt = createBiometricPrompt()
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
        )[AuthSetupBiometricsViewModel::class.java]

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
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
    }

    private fun initializeViews() {
        hideActionBarBack(false)
        binding.enableBiometricsButton.setOnClickListener {
            onEnableBiometricsClicked()
        }
        binding.cancelButton.setOnClickListener {
            onCancelClicked()
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun onEnableBiometricsClicked() {
        val promptInfo = createPromptInfo()

        val cipher = viewModel.getCipherForBiometrics()
        if (cipher != null) {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun onCancelClicked() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    //endregion

    //region Biometrics
    // ************************************************************

    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPromptCallback() {
            override fun onAuthenticationSucceeded(cipher: Cipher) {
                viewModel.setupBiometricWithPassword(cipher)
            }
        }

        return BiometricPrompt(this, executor, callback)
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_setup_biometrics_dialog_title))
            .setSubtitle(getString(R.string.auth_setup_biometrics_dialog_subtitle))
            .setConfirmationRequired(false)
            .setNegativeButtonText(getString(R.string.auth_setup_biometrics_dialog_cancel))
            .build()
    }

    //endregion


    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }
}
