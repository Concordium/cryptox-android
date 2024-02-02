package com.concordium.wallet.ui.more.alterpassword

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityAlterpasswordBinding
import com.concordium.wallet.ui.auth.setup.AuthSetupActivity
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

    private val REQUESTCODE_AUTH_RESET = 3001

    private val viewModel: AlterPasswordViewModel by viewModels()
    private val binding by lazy {
        ActivityAlterpasswordBinding.bind(findViewById(R.id.root_layout))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()

        hideActionBarBack(isVisible = true)

        showWaiting(false)

        binding.confirmButton.setOnClickListener {
            viewModel.checkAndStartPasscodeChange()
        }

        viewModel.checkAccountsIdentitiesDoneLiveData.observe(this) { success ->
            if (success) {
                showAuthentication(
                    activity = this@AlterPasswordActivity,
                    onAuthenticated = viewModel::checkLogin
                )
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.alterpassword_non_finalised_items),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showErrorMessage(value)
            }
        })

        viewModel.doneInitialAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                val intent = Intent(baseContext, AuthSetupActivity::class.java)
                intent.putExtra(AuthSetupActivity.CONTINUE_INITIAL_SETUP, false)
                startActivityForResult(intent, REQUESTCODE_AUTH_RESET)
            }
        })

        viewModel.errorInitialAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                Toast.makeText(baseContext, getString(R.string.change_password_initial_error), Toast.LENGTH_LONG).show()
            }
        })

        viewModel.doneFinalChangePasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                Toast.makeText(baseContext, getString(R.string.change_password_successfully_changed), Toast.LENGTH_LONG).show()
                finish()
            }
        })

        viewModel.errorFinalChangePasswordLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                Toast.makeText(baseContext, getString(R.string.change_password_final_error), Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUESTCODE_AUTH_RESET) {
            if (resultCode == Activity.RESULT_OK) {
                App.appCore.session.tempPassword?.let {
                    viewModel.finishPasswordChange(it)
                }
            }
        }
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
