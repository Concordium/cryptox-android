package com.concordium.wallet.ui.auth.setup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAuthSetupBinding
import com.concordium.wallet.ui.auth.setupbiometrics.AuthSetupBiometricsActivity
import com.concordium.wallet.ui.auth.setuppassword.AuthSetupPasswordActivity
import com.concordium.wallet.ui.auth.setuprepeat.AuthSetupRepeatActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.view.PasscodeView
import com.concordium.wallet.util.KeyboardUtil

class AuthSetupActivity : BaseActivity(
    R.layout.activity_auth_setup,
    R.string.auth_setup_title
) {

    private var continueFlow: Boolean = true

    companion object {
        const val CONTINUE_INITIAL_SETUP = "CONTINUE_INITIAL_SETUP"
    }

    private lateinit var binding: ActivityAuthSetupBinding
    private lateinit var viewModel: AuthSetupViewModel

    //region Lifecycle
    //************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthSetupBinding.bind(findViewById(R.id.root_layout))

        continueFlow = intent.getBooleanExtra(CONTINUE_INITIAL_SETUP, true)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()

        hideActionBarBack(isVisible = false)
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    private fun finishSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    //endregion

    //region Initialize
    //************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AuthSetupViewModel::class.java]
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
                    finishSuccess()
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
        binding.passcodeView.passcodeListener = object : PasscodeView.PasscodeListener {
            override fun onInputChanged() {
                binding.errorTextview.text = ""
                binding.errorTextview.isVisible = false
            }

            override fun onDone() {
                onConfirmClicked()
            }
        }
        binding.fullPasswordButton.setOnClickListener {
            gotoAuthSetupPassword()
        }
        binding.passcodeView.requestFocus()
    }

    //endregion

    //region Control/UI
    //************************************************************

    private fun onConfirmClicked() {
        if (viewModel.checkPasswordRequirements(binding.passcodeView.getPasscode())) {
            viewModel.startSetupPassword(binding.passcodeView.getPasscode())
            gotoAuthSetupPasscodeRepeat()
        } else {
            binding.passcodeView.clearPasscode()
            binding.errorTextview.setText(R.string.auth_error_passcode_not_valid)
            binding.errorTextview.isVisible = true
        }
    }

    private val getResultAuthSetupBiometrics =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (continueFlow) {
                    viewModel.hasFinishedSetupPassword()
                }
                finishSuccess()
            }
        }

    private fun gotoAuthSetupBiometrics() {
        val intent = Intent(this, AuthSetupBiometricsActivity::class.java)
        getResultAuthSetupBiometrics.launch(intent)
    }

    private val getResultAuthSetupPasscodeRepeat =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val result = it.data
            if (it.resultCode == Activity.RESULT_OK && result != null) {
                if (AuthSetupRepeatActivity.useFullPassword(result)) {
                    gotoAuthSetupPassword()
                } else if (AuthSetupRepeatActivity.doesMatch(result)) {
                    viewModel.setupPassword(binding.passcodeView.getPasscode(), continueFlow)
                } else {
                    binding.passcodeView.clearPasscode()
                    binding.errorTextview.setText(R.string.auth_error_passcodes_different)
                    binding.errorTextview.isVisible = true
                }
            }
        }

    private fun gotoAuthSetupPasscodeRepeat() {
        val intent = Intent(this, AuthSetupRepeatActivity::class.java)
        getResultAuthSetupPasscodeRepeat.launch(intent)
    }

    private val getResultAuthSetupFullPassword =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                if (continueFlow) {
                    viewModel.hasFinishedSetupPassword()
                }
                finishSuccess()
            }
            binding.passcodeView.clearPasscode()
        }

    private fun gotoAuthSetupPassword() {
        val intent = Intent(this, AuthSetupPasswordActivity::class.java)
        getResultAuthSetupFullPassword.launch(intent)
    }

    private fun showPasswordError() {
        binding.passcodeView.clearPasscode()
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.rootLayout, R.string.auth_error_password_setup)
    }

    override fun loggedOut() {
    }

    //endregion
}
