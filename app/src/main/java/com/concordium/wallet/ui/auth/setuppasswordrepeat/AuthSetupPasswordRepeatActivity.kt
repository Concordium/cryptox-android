package com.concordium.wallet.ui.auth.setuppasswordrepeat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAuthSetupPasswordBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.afterTextChanged

class AuthSetupPasswordRepeatActivity : BaseActivity(
    R.layout.activity_auth_setup_password,
    R.string.auth_setup_password_repeat_title
) {

    private lateinit var viewModel: AuthSetupPasswordRepeatViewModel
    private val binding by lazy {
        ActivityAuthSetupPasswordBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()

        hideActionBarBack(isVisible = true)
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AuthSetupPasswordRepeatViewModel::class.java]

        viewModel.finishScreenLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                setResult(
                    Activity.RESULT_OK,
                    createResult(
                        doesMatch = value,
                        useFullPassword = false,
                    )
                )
                finish()
            }
        })
    }

    private fun initializeViews() {
        binding.instructionTextview.setText(R.string.auth_setup_password_repeat_info)
        binding.confirmButton.setOnClickListener {
            onConfirmClicked()
        }
        binding.passwordEdittext.afterTextChanged {
            binding.confirmButton.isVisible = it.isNotEmpty()
            binding.passcodeButton.isVisible = it.isEmpty()
        }
        binding.passwordEdittext.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    onConfirmClicked()
                    true
                }

                else -> false
            }
        }
        binding.passcodeButton.setOnClickListener {
            setResult(
                Activity.RESULT_OK,
                createResult(
                    doesMatch = false,
                    useFullPassword = true,
                )
            )
            finish()
        }
        binding.passwordEdittext.requestFocus()
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun onConfirmClicked() {
        viewModel.checkPassword(binding.passwordEdittext.text.toString())
    }

    //endregion


    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }

    companion object {
        private const val DOES_MATCH_EXTRA = "does_match"
        private const val USE_PASSCODE_EXTRA = "use_passcode"

        private fun createResult(
            doesMatch: Boolean,
            useFullPassword: Boolean,
        ) = Intent().apply {
            putExtra(DOES_MATCH_EXTRA, doesMatch)
            putExtra(USE_PASSCODE_EXTRA, useFullPassword)
        }

        fun doesMatch(result: Intent): Boolean =
            result.getBooleanExtra(DOES_MATCH_EXTRA, false)

        fun usePasscode(result: Intent): Boolean =
            result.getBooleanExtra(USE_PASSCODE_EXTRA, false)
    }
}
