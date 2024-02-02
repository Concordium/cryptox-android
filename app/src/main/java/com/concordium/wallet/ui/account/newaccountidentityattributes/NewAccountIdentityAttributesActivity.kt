package com.concordium.wallet.ui.account.newaccountidentityattributes

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
import com.concordium.wallet.databinding.ActivityNewAccountIdentityAttributesBinding
import com.concordium.wallet.ui.account.newaccountconfirmed.NewAccountConfirmedActivity
import com.concordium.wallet.ui.account.newaccountsetup.NewAccountSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel
import com.concordium.wallet.uicore.dialog.AuthenticationDialogFragment
import com.concordium.wallet.util.PrettyPrint.prettyPrint

class NewAccountIdentityAttributesActivity : BaseActivity(
    R.layout.activity_new_account_identity_attributes,
    R.string.new_account_identity_attributes_title
), AuthDelegate by AuthDelegateImpl() {

    companion object {
        const val EXTRA_ACCOUNT_NAME = "EXTRA_ACCOUNT_NAME"
        const val EXTRA_IDENTITY = "EXTRA_IDENTITY"
    }

    private lateinit var viewModel: NewAccountIdentityAttributesViewModel
    private val binding by lazy {
        ActivityNewAccountIdentityAttributesBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var biometricPrompt: BiometricPrompt
    private var identityAttributeAdapter: IdentityAttributeAdapter = IdentityAttributeAdapter()

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accountName =
            intent.getStringExtra(NewAccountSetupActivity.EXTRA_ACCOUNT_NAME) as String
        val identity = intent.getSerializableExtra(EXTRA_IDENTITY) as Identity

        initializeViewModel()
        viewModel.initialize(accountName, identity)
        initViews()

        hideActionBarBack(isVisible = true)
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NewAccountIdentityAttributesViewModel::class.java]

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
                        activity = this@NewAccountIdentityAttributesActivity,
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
        viewModel.identityAttributeListLiveData.observe(this) { identityAttributeList ->
            identityAttributeList?.let {
                identityAttributeAdapter.setData(it)
            }
        }
    }

    private fun initViews() {
        binding.progress.progressLayout.visibility = View.GONE

        binding.identityView.setIdentityData(viewModel.identity)

        identityAttributeAdapter.setOnItemClickListener(object :
            IdentityAttributeAdapter.OnItemClickListener {
            override fun onItemClicked(item: SelectableIdentityAttribute) {
            }

            override fun onCheckedChanged(item: SelectableIdentityAttribute) {
            }
        })
        binding.attributesRecyclerview.adapter = identityAttributeAdapter
        binding.attributesRecyclerview.isNestedScrollingEnabled = false
        binding.confirmButton.setOnClickListener {
            binding.confirmButton.isEnabled = false
            viewModel.confirmSelectedAttributes(identityAttributeAdapter.getCheckedAttributes())
        }
    }

    //endregion

    //region Control
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            prettyPrint()
            binding.progress.progressLayout.visibility = View.VISIBLE
            binding.confirmButton.isEnabled = false
        } else {
            binding.progress.progressLayout.visibility = View.GONE
            binding.confirmButton.isEnabled = true
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

    private fun showPasswordDialog() {
        val dialogFragment = AuthenticationDialogFragment()
        dialogFragment.setCallback(object : AuthenticationDialogFragment.Callback {
            override fun onCorrectPassword(password: String) {
                viewModel.continueWithPassword(password)
            }

            override fun onCancelled() {
            }
        })
        dialogFragment.show(supportFragmentManager, AuthenticationDialogFragment.AUTH_DIALOG_TAG)
    }

    //endregion
}
