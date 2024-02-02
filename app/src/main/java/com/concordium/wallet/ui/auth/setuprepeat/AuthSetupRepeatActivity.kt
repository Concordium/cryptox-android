package com.concordium.wallet.ui.auth.setuprepeat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAuthSetupBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.view.PasscodeView

class AuthSetupRepeatActivity : BaseActivity(
    R.layout.activity_auth_setup,
    R.string.auth_setup_repeat_title
) {
    private lateinit var viewModel: AuthSetupRepeatViewModel
    private val binding by lazy {
        ActivityAuthSetupBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()

        hideActionBarBack(isVisible = false)
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AuthSetupRepeatViewModel::class.java]

        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                setResult(
                    Activity.RESULT_OK,
                    createResult(
                        doesMatch = value,
                        useFullPassword = false
                    )
                )
                finish()
            }
        })
    }

    private fun initializeViews() {
        binding.instructionTextview.setText(R.string.auth_setup_repeat_info)
        binding.passcodeView.passcodeListener = object : PasscodeView.PasscodeListener {
            override fun onInputChanged() {
            }

            override fun onDone() {
                onConfirmClicked()
            }
        }
        binding.fullPasswordButton.setOnClickListener {
            setResult(
                Activity.RESULT_OK,
                createResult(
                    doesMatch = false,
                    useFullPassword = true
                )
            )
            finish()
        }
        binding.passcodeView.requestFocus()
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun onConfirmClicked() {
        viewModel.checkPassword(binding.passcodeView.getPasscode())
    }

    //endregion

    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }

    companion object {
        private const val DOES_MATCH_EXTRA = "does_match"
        private const val USE_FULL_PASSWORD_EXTRA = "use_full_password"

        private fun createResult(
            doesMatch: Boolean,
            useFullPassword: Boolean,
        ) = Intent().apply {
            putExtra(DOES_MATCH_EXTRA, doesMatch)
            putExtra(USE_FULL_PASSWORD_EXTRA, useFullPassword)
        }

        fun doesMatch(result: Intent): Boolean =
            result.getBooleanExtra(DOES_MATCH_EXTRA, false)

        fun useFullPassword(result: Intent): Boolean =
            result.getBooleanExtra(USE_FULL_PASSWORD_EXTRA, false)
    }
}
