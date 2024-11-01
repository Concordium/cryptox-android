package com.concordium.wallet.ui.identity.identityconfirmed

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.databinding.ActivityIdentityConfirmedBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.common.account.BaseAccountActivity
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegate
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegateImpl
import com.concordium.wallet.util.getSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IdentityConfirmedActivity :
    BaseAccountActivity(
        R.layout.activity_identity_confirmed,
        R.string.identity_confirmed_title
    ),
    IdentityStatusDelegate by IdentityStatusDelegateImpl() {

    private lateinit var binding: ActivityIdentityConfirmedBinding
    private lateinit var viewModel: IdentityConfirmedViewModel
    private var showForFirstAccount = false
    private var showForCreateAccount = false
    private lateinit var identity: Identity
    private var accountCreated = false

    companion object {
        const val EXTRA_IDENTITY = "EXTRA_IDENTITY"
        const val SHOW_FOR_CREATE_FIRST_ACCOUNT = "SHOW_FOR_CREATE_FIRST_ACCOUNT"
        const val SHOW_FOR_CREATE_ACCOUNT = "SHOW_FOR_CREATE_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityConfirmedBinding.bind(findViewById(R.id.root_layout))

        showForFirstAccount = intent.extras?.getBoolean(SHOW_FOR_CREATE_FIRST_ACCOUNT, false) ?: false
        showForCreateAccount = intent.extras?.getBoolean(SHOW_FOR_CREATE_ACCOUNT, false) ?: false

        setActionBarTitle("")
        hideActionBarBack(isVisible = true)

        identity = requireNotNull(intent.getSerializable(EXTRA_IDENTITY, Identity::class.java))

        initializeNewAccountViewModel()
        initializeAuthenticationObservers()
        initializeViewModel()
        initializeViews()

        // If we're being restored from a previous state
        if (savedInstanceState != null) {
            return
        }

        App.appCore.tracker.identityVerificationResultScreen()

        if (!showForFirstAccount) {
            showRequestNoticeDialog(IdentityRequestNoticeDialog.submitted())
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateState()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCheckForPendingIdentity()
        if (showForFirstAccount)
            viewModel.stopIdentityUpdate()
    }

    override fun onBackPressed() {
        if (showForFirstAccount || (showForCreateAccount && !accountCreated))
            super.onBackPressed()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IdentityConfirmedViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
    }

    private fun initializeViews() {
        showWaiting(true)

        binding.confirmButton.setOnClickListener {
            gotoAccountsOverview()
        }

        identity.let {
            binding.identityView.setIdentityData(it)
            startCheckForPendingIdentity(this, it.id, false) { newIdentity ->
                stopCheckForPendingIdentity()

                identity = newIdentity
                binding.identityView.setIdentityData(newIdentity)

                when (newIdentity.status) {
                    IdentityStatus.DONE -> {
                        App.appCore.tracker.identityVerificationResultApprovedDialog()
                        showRequestNoticeDialog(IdentityRequestNoticeDialog.approved())
                    }

                    IdentityStatus.ERROR -> {
                        dismissAnyRequestNoticeDialog()
                    }
                }

                showSubmitAccount()
            }
        }

        binding.btnSubmitAccount.visibility = View.GONE

        if (showForFirstAccount) {
            binding.confirmButton.isVisible = false
            binding.btnSubmitAccount.isVisible = false
            createAccount(identity)
        } else {
            if (showForCreateAccount) {
                showSubmitAccount()
            }
            binding.confirmButton.isVisible = true
            binding.confirmButton.text = getString(R.string.identity_confirmed_finish_button)
        }

        binding.btnSubmitAccount.setOnClickListener {
            App.appCore.tracker.identityVerificationResultCreateAccountClicked()
            createAccount(identity)
        }
    }

    private fun gotoAccountsOverview() {
        finishAffinity()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.includeProgress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.includeProgress.progressLayout.visibility = View.GONE
        }
    }

    override fun showError(stringRes: Int) {
        // error will be shown on account overview page later
    }

    override fun accountCreated(account: Account) {
        if (showForFirstAccount) {
            finishAffinity()
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } else {
            accountCreated = true
            hideActionBarBack(isVisible = false)
            binding.confirmButton.visibility = View.VISIBLE
        }
    }

    private fun showSubmitAccount() {
        CoroutineScope(Dispatchers.IO).launch {
            val identityRepository =
                IdentityRepository(WalletDatabase.getDatabase(application).identityDao())
            identity = checkNotNull(identityRepository.findById(identity.id)) {
                "The identity ${identity.id} must be in the repository"
            }
            runOnUiThread {
                identity.let {
                    binding.identityView.setIdentityData(it)
                    binding.btnSubmitAccount.isEnabled = it.status == IdentityStatus.DONE
                    binding.confirmButton.visibility = View.GONE
                    binding.btnSubmitAccount.visibility = View.VISIBLE
                    if (showForCreateAccount) {
                        setActionBarTitle(R.string.identity_confirmed_create_new_account)
                    }
                }
            }
        }
    }

    private fun dismissAnyRequestNoticeDialog() {
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment.tag == IdentityRequestNoticeDialog.TAG && fragment is DialogFragment) {
                fragment.dismissAllowingStateLoss()
            }
        }
    }

    private fun showRequestNoticeDialog(dialog: IdentityRequestNoticeDialog) {
        dismissAnyRequestNoticeDialog()
        dialog.show(supportFragmentManager, IdentityRequestNoticeDialog.TAG)
    }

    private fun createAccount(identity: Identity) {
        CoroutineScope(Dispatchers.IO).launch {
            runOnUiThread {
                viewModelNewAccount.initialize(Account.getDefaultName(""), identity)
                viewModelNewAccount.createAccount()
            }
        }
    }
}
