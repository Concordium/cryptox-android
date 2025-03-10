package com.concordium.wallet.ui.auth.setup

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.security.BiometricPromptCallback
import com.concordium.wallet.databinding.DialogAuthSetupBiometricsBinding
import javax.crypto.Cipher

class AuthSetupBiometricsDialog : AppCompatDialogFragment() {
    override fun getTheme(): Int =
        R.style.CCX_Dialog

    private lateinit var binding: DialogAuthSetupBiometricsBinding
    private lateinit var viewModel: AuthSetupBiometricsViewModel
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAuthSetupBiometricsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AuthSetupBiometricsViewModel::class.java]
        viewModel.initialize()
        viewModel.finishScreenLiveData.observe(viewLifecycleOwner) {
            dismiss()
        }

        biometricPrompt = createBiometricPrompt()

        listOf(binding.denyButton, binding.closeButton).forEach {
            it.setOnClickListener {
                App.appCore.tracker.welcomePasscodeBiometricsRejected()
                dismiss()
            }
        }

        binding.allowButton.setOnClickListener {
            val promptInfo = createPromptInfo()

            val cipher = viewModel.getCipherForBiometrics()
            if (cipher != null) {
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            }

            App.appCore.tracker.welcomePasscodeBiometricsAccepted()
        }

        if (savedInstanceState == null) {
            App.appCore.tracker.welcomePasscodeBiometricsDialog()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
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
