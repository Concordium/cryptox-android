package com.concordium.wallet.ui.account.newaccountsetup

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityNewAccountSetupBinding
import com.concordium.wallet.ui.account.newaccountconfirmed.NewAccountConfirmedActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel

class NewAccountSetupActivity : BaseActivity(
    R.layout.activity_new_account_setup,
    R.string.app_name,
), AuthDelegate by AuthDelegateImpl() {

    companion object {
        const val EXTRA_ACCOUNT_NAME = "EXTRA_ACCOUNT_NAME"
        const val EXTRA_IDENTITY = "EXTRA_IDENTITY"
    }

    private lateinit var viewModel: NewAccountSetupViewModel
    private val binding by lazy {
        ActivityNewAccountSetupBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var accountName: String
    private lateinit var identity: Identity

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME) as String
        identity = intent.getSerializableExtra(EXTRA_IDENTITY) as Identity

        initializeViewModel()
        viewModel.initialize(accountName, identity)
        initViews()

        hideActionBarBack(isVisible = true)
        setActionBarTitle(accountName)
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NewAccountSetupViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(
                        activity = this@NewAccountSetupActivity,
                        onAuthenticated = viewModel::continueWithPassword
                    )
                }
            }
        })
        viewModel.gotoAccountCreatedLiveData.observe(this, object : EventObserver<Account>() {
            override fun onUnhandledEvent(value: Account) {
                gotoNewAccountConfirmed(value)
            }
        })
        viewModel.gotoFailedLiveData.observe(
            this,
            object : EventObserver<Pair<Boolean, BackendError?>>() {
                override fun onUnhandledEvent(value: Pair<Boolean, BackendError?>) {
                    if (value.first) {
                        gotoFailed(value.second)
                    }
                }
            })
    }

    private fun initViews() {
        binding.identityView.setIdentityData(identity)

        binding.progress.progressLayout.visibility = View.GONE

        binding.confirmSubmitButton.setOnClickListener {
            binding.confirmSubmitButton.isEnabled = false
            viewModel.createAccount()
        }
    }

    //endregion

    //region Control
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
            binding.confirmSubmitButton.isEnabled = false
        } else {
            binding.progress.progressLayout.visibility = View.GONE
            binding.confirmSubmitButton.isEnabled = true
        }
    }

    private fun gotoNewAccountConfirmed(account: Account) {
        val intent = Intent(this, NewAccountConfirmedActivity::class.java)
        intent.putExtra(NewAccountConfirmedActivity.EXTRA_ACCOUNT, account)
        startActivity(intent)
    }

    private fun gotoFailed(error: BackendError?) {
        val intent = Intent(this, FailedActivity::class.java)
        intent.putExtra(FailedActivity.EXTRA_SOURCE, FailedViewModel.Source.Account)
        error?.let {
            intent.putExtra(FailedActivity.EXTRA_ERROR, it)
        }
        startActivity(intent)
    }

    //endregion
}
