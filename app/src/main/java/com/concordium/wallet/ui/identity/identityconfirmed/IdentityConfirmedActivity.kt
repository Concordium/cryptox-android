package com.concordium.wallet.ui.identity.identityconfirmed

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
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
import com.concordium.wallet.uicore.dialog.Dialogs
import com.concordium.wallet.util.getSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IdentityConfirmedActivity :
    BaseAccountActivity(
        R.layout.activity_identity_confirmed,
        R.string.identity_confirmed_title
    ),
    IdentityStatusDelegate by IdentityStatusDelegateImpl(),
    Dialogs.DialogFragmentListener {

    private lateinit var binding: ActivityIdentityConfirmedBinding
    private lateinit var viewModel: IdentityConfirmedViewModel
    private var showForFirstIdentity = false
    private var showForCreateAccount = false
    private lateinit var identity: Identity
    private var accountCreated = false

    companion object {
        const val EXTRA_IDENTITY = "EXTRA_IDENTITY"

        // TODO: This screen needs to be refactored.
        // There is too much logic implicitly relying on
        // IdentityUpdater and IdentityStatusDelegate behaviour
        // as well depending on whether this screen is opened
        // to create first identity or account.
        // I'm struggling to figure it out and some parts seem broken.
        const val SHOW_FOR_FIRST_IDENTITY = "SHOW_FOR_FIRST_IDENTITY"
        const val SHOW_FOR_CREATE_ACCOUNT = "SHOW_FOR_CREATE_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityConfirmedBinding.bind(findViewById(R.id.root_layout))

        showForFirstIdentity = intent.extras?.getBoolean(SHOW_FOR_FIRST_IDENTITY, false) ?: false
        showForCreateAccount = intent.extras?.getBoolean(SHOW_FOR_CREATE_ACCOUNT, false) ?: false

        setActionBarTitle("")
        hideActionBarBack(isVisible = !showForFirstIdentity)

        identity = requireNotNull(intent.getSerializable(EXTRA_IDENTITY, Identity::class.java))

        initializeNewAccountViewModel()
        initializeAuthenticationObservers()
        initializeViewModel()
        initializeViews()

        // If we're being restored from a previous state
        if (savedInstanceState != null) {
            return
        }

        showRequestNoticeDialog(IdentityRequestNoticeDialog.submitted())

        if (showForFirstIdentity)
            viewModel.startIdentityUpdate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateState()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCheckForPendingIdentity()
        if (showForFirstIdentity)
            viewModel.stopIdentityUpdate()
    }

    override fun onBackPressed() {
        if (!showForFirstIdentity && showForCreateAccount && !accountCreated)
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
            if (showForFirstIdentity) {
                showSubmitAccount()
            } else {
                gotoAccountsOverview()
            }
        }

        identity.let {
            binding.identityView.setIdentityData(it)
            startCheckForPendingIdentity(this, it.id, showForFirstIdentity) { newIdentity ->
                stopCheckForPendingIdentity()

                identity = newIdentity
                binding.identityView.setIdentityData(newIdentity)

                if (newIdentity.status == IdentityStatus.DONE) {
                    showRequestNoticeDialog(IdentityRequestNoticeDialog.approved())
                }

                showSubmitAccount()
            }
        }

        binding.btnSubmitAccount.visibility = View.GONE

        if (showForFirstIdentity) {
            binding.confirmButton.isVisible = false
        } else {
            if (showForCreateAccount) {
                showSubmitAccount()
            }
            binding.confirmButton.isVisible = true
            binding.confirmButton.text = getString(R.string.identity_confirmed_finish_button)
        }

        binding.btnSubmitAccount.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                runOnUiThread {
                    viewModelNewAccount.initialize(Account.getDefaultName(""), identity)
                    viewModelNewAccount.confirmWithoutAttributes()
                }
            }
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
        if (showForFirstIdentity) {
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

    private fun showRequestNoticeDialog(dialog: IdentityRequestNoticeDialog) {
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment.tag == IdentityRequestNoticeDialog.TAG && fragment is DialogFragment) {
                fragment.dismissAllowingStateLoss()
            }
        }
        dialog.show(supportFragmentManager, IdentityRequestNoticeDialog.TAG)
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        // No use of this, yet currently required to have.
    }
}
