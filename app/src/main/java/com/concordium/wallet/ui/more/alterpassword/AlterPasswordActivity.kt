package com.concordium.wallet.ui.more.alterpassword

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAlterpasswordBinding
import com.concordium.wallet.ui.auth.passcode.PasscodeSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.KeyboardUtil

class AlterPasswordActivity : BaseActivity(
    R.layout.activity_alterpassword,
    R.string.alterpassword_title
), AuthDelegate by AuthDelegateImpl() {

    //region Lifecycle
    // ************************************************************

    private val viewModel: AlterPasswordViewModel by viewModels()
    private val binding by lazy {
        ActivityAlterpasswordBinding.bind(findViewById(R.id.root_layout))
    }
    private val passcodeSetupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onPasscodeSetupResult(
                isSetUpSuccessfully = it.resultCode==Activity.RESULT_OK
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()

        hideActionBarBack(isVisible = true)

        showWaiting(false)

        binding.confirmButton.setOnClickListener {
            showAuthentication(
                activity = this@AlterPasswordActivity,
                onAuthenticated = viewModel::onAuthenticated
            )
        }

        viewModel.waitingLiveData.observe(this, ::showWaiting)

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showErrorMessage(value)
            }
        })

        viewModel.doneInitialAuthenticationLiveData.observe(
            this,
            object : EventObserver<Boolean>() {
                override fun onUnhandledEvent(value: Boolean) {
                    passcodeSetupLauncher.launch(
                        Intent(
                            this@AlterPasswordActivity,
                            PasscodeSetupActivity::class.java
                        )
                    )
                }
            })

        viewModel.errorInitialAuthenticationLiveData.observe(
            this,
            object : EventObserver<Boolean>() {
                override fun onUnhandledEvent(value: Boolean) {
                    Toast.makeText(
                        baseContext,
                        getString(R.string.change_password_initial_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        viewModel.doneFinalChangePasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                Toast.makeText(
                    baseContext,
                    getString(R.string.change_password_successfully_changed),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        })

        viewModel.errorFinalChangePasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                Toast.makeText(
                    baseContext,
                    getString(R.string.change_password_final_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun showErrorMessage(stringRes: Int) {
        KeyboardUtil.hideKeyboard(this)
        popup.showSnackbar(binding.root, stringRes)
    }

    private fun initializeViewModel() {
    }

    //endregion

    override fun loggedOut() {
        // No need to show auth, as it is anyway requested further.
    }
}
