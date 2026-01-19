package com.concordium.wallet.ui.auth.setup

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.uicore.dialog.BaseDialogFragment
import javax.crypto.Cipher

class AuthSetupBiometricsDialog : BaseDialogFragment() {

    private lateinit var viewModel: AuthSetupBiometricsViewModel
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AuthSetupBiometricsViewModel::class.java]
        viewModel.initialize()
        viewModel.finishScreenLiveData.observe(viewLifecycleOwner) {
            dismiss()
        }

        setViews(
            title = getString(R.string.passcode_create_setup_biometrics_title),
            description = getString(R.string.passcode_create_setup_biometrics_details),
            okButtonText = getString(R.string.passcode_create_setup_biometrics_allow),
            cancelButtonText = getString(R.string.passcode_create_setup_biometrics_deny),
            iconResId = R.drawable.mw24_ic_biometrics
        )

        biometricPrompt = createBiometricPrompt()

        listOf(binding.cancelButton, binding.closeButton).forEach {
            it.setOnClickListener {
                App.appCore.tracker.passcodeBiometricsRejected()
                dismiss()
            }
        }

        binding.okButton.setOnClickListener {
            val promptInfo = createPromptInfo()

            val cipher = viewModel.getCipherForBiometrics()
            if (cipher != null) {
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            }

            App.appCore.tracker.passcodeSetupBiometricsAccepted()
        }
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.passcodeSetupBiometricsDialog()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        App.appCore.tracker.passcodeBiometricsRejected()
        (activity as? DialogInterface.OnDismissListener)?.onDismiss(dialog)
    }

    //region Biometrics
    // ************************************************************

    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(requireContext())

        val callback = object : BiometricPromptCallback() {
            override fun onAuthenticationSucceeded(cipher: Cipher) {
                viewModel.proceedWithSetup(cipher)
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

    companion object {
        const val TAG = "passcode-setup-biometrics"
    }
}
