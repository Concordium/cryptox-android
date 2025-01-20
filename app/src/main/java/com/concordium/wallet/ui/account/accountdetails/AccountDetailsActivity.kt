package com.concordium.wallet.ui.account.accountdetails

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAccountDetailsBinding
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsErrorFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsPendingFragment
import com.concordium.wallet.ui.account.accountdetails.transfers.AccountDetailsTransfersActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenActivity
import com.concordium.wallet.ui.cis2.TokenDetailsActivity
import com.concordium.wallet.ui.cis2.TokensFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EarnDelegateImpl
import com.concordium.wallet.ui.onramp.CcdOnrampSitesActivity
import java.math.BigInteger

class AccountDetailsActivity : BaseActivity(
    R.layout.activity_account_details,
    R.string.account_details_title
), EarnDelegate by EarnDelegateImpl(), AuthDelegate by AuthDelegateImpl() {

    private val binding by lazy {
        ActivityAccountDetailsBinding.bind(findViewById(R.id.root_layout))
    }

    lateinit var viewModelAccountDetails: AccountDetailsViewModel
        private set
    lateinit var viewModelTokens: TokensViewModel
        private set

    private val shouldOpenTokens: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_OPEN_TOKENS, false)
    }
    private val tokenToOpenUid: String? by lazy {
        intent.getStringExtra(EXTRA_TOKEN_TO_OPEN_UID)
    }

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_TOKEN_TO_OPEN_UID = "EXTRA_TOKEN_TO_OPEN_UID"
        const val EXTRA_OPEN_TOKENS = "EXTRA_OPEN_TOKENS"
        const val RESULT_RETRY_ACCOUNT_CREATION = 2
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras?.getSerializable(EXTRA_ACCOUNT) as Account
        initializeViewModel()
//        viewModelAccountDetails.initialize(account)
        initializeViewModelTokens()
        initViews()
        hideActionBarBack(isVisible = true)
        hideSettings(isVisible = true) {
            goToAccountSettings()
        }
    }

    override fun onResume() {
        super.onResume()
//        viewModelAccountDetails.populateTransferList()
        viewModelAccountDetails.initiateFrequentUpdater()
    }

    override fun onPause() {
        super.onPause()
        viewModelAccountDetails.stopFrequentUpdater()
    }
    // endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModelAccountDetails = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountDetailsViewModel::class.java]

        viewModelAccountDetails.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModelAccountDetails.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModelAccountDetails.finishLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                finish()
            }
        })
        viewModelAccountDetails.totalBalanceLiveData.observe(this, ::showTotalBalance)

        viewModelAccountDetails.showPadLockLiveData.observe(this) {
            invalidateOptionsMenu()
        }

        viewModelAccountDetails.accountUpdatedLiveData.observe(this) {
            // Update balances in the title and on the Tokens tab.
            initTitle()
            viewModelTokens.tokenData.account = viewModelAccountDetails.account
            viewModelTokens.loadTokensBalances()
        }
    }

    private fun initializeViewModelTokens() {
        viewModelTokens = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TokensViewModel::class.java]

        viewModelTokens.tokenData.account = viewModelAccountDetails.account

        viewModelTokens.chooseToken.observe(this) { token ->
            showTokenDetailsDialog(token)
        }

        viewModelTokens.tokenBalances.observe(this, object : Observer<Boolean> {
            override fun onChanged(t: Boolean) {
                // Open the requested token once, when balances are loaded.
                if (tokenToOpenUid != null) {
                    viewModelTokens.tokenBalances.removeObserver(this)
                    viewModelTokens.tokens
                        .find { it.uid == tokenToOpenUid }
                        ?.also(viewModelTokens.chooseToken::postValue)
                }
            }
        })
    }

    private fun initViews() {
        initTitle()
        showWaiting(false)
        when (viewModelAccountDetails.account.transactionStatus) {
            TransactionStatus.ABSENT -> {
                setErrorMode()
            }

            TransactionStatus.FINALIZED -> {
                setFinalizedMode()
            }

            TransactionStatus.COMMITTED -> setPendingMode()
            TransactionStatus.RECEIVED -> setPendingMode()
            else -> {
            }
        }
        binding.accountRetryButton.setOnClickListener {
            setResult(RESULT_RETRY_ACCOUNT_CREATION)
            finish()
        }
        binding.accountRemoveButton.setOnClickListener {
            viewModelAccountDetails.deleteAccountAndFinish()
        }

        binding.onrampBtn.setOnClickListener {
            onOnrampClicked()
        }

        binding.sendFundsBtn.setOnClickListener {
            onSendFundsClicked()
        }

        binding.receiveBtn.setOnClickListener {
            onAddressClicked()
        }

        binding.earnBtn.setOnClickListener {
            onEarnClicked()
        }

        binding.activityBtn.setOnClickListener {
            onActivityClicked()
        }
        initContainer()
    }

    private fun initTitle() {
        setActionBarTitle(
            getString(
                R.string.account_details_title_regular_balance,
                viewModelAccountDetails.account.getAccountName()
            )
        )
    }

    private fun setFinalizedMode() {
        binding.onrampBtn.isEnabled = true
        binding.sendFundsBtn.isEnabled = !viewModelAccountDetails.account.readOnly
        binding.receiveBtn.isEnabled = true
        binding.earnBtn.isEnabled = !viewModelAccountDetails.account.readOnly

        setupOnrampBanner(active = true)
    }

    private fun setErrorMode() {
        setPendingMode()
        binding.accountRetryButton.visibility = View.VISIBLE
        binding.accountRemoveButton.visibility = View.VISIBLE
    }

    private fun setPendingMode() {
        binding.onrampBtn.isEnabled = false
        binding.sendFundsBtn.isEnabled = false
        binding.receiveBtn.isEnabled = false
        binding.earnBtn.isEnabled = false
        binding.activityBtn.isEnabled = false
        setupOnrampBanner(active = false)
    }

    private fun replaceTokensFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.tokensFragmentContainer.id, fragment)
            .commit()
    }

    private fun initContainer() {
        var handledContainerHeight = -1
        binding.scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val containerHeight = binding.scrollView.measuredHeight
            val buttonsHeight = binding.buttonsBlock.measuredHeight
            val onRampHeight = binding.onrampBanner.root.measuredHeight
            val buttonsMargin =
                (binding.tokensFragmentContainer.layoutParams as MarginLayoutParams).topMargin
            val onRampMargin =
                (binding.onrampBanner.root.layoutParams as MarginLayoutParams).topMargin

            if (handledContainerHeight != containerHeight) {
                handledContainerHeight = containerHeight

                binding.tokensFragmentContainer.updateLayoutParams<ViewGroup.LayoutParams> {
                    height =
                        containerHeight - buttonsHeight - buttonsMargin - onRampMargin - onRampHeight
                }
            }
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun goToAccountSettings() {
        startActivity(Intent(this, AccountSettingsActivity::class.java).apply {
            putExtra(AccountSettingsActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
        })
    }

    private fun showTotalBalance(totalBalance: BigInteger) {
        binding.totalBalanceTextview.text = getString(
            R.string.account_details_total_balance,
            CurrencyUtil.formatAndRoundGTU(
                value = totalBalance,
                roundDecimals = 2
            )
        )
        if (viewModelAccountDetails.account.balanceAtDisposal != totalBalance) {
            binding.atDisposalLabel.visibility = View.VISIBLE
            binding.atDisposalLabel.text = getString(
                R.string.account_details_balance_at_disposal,
                CurrencyUtil.formatGTU(viewModelAccountDetails.account.balanceAtDisposal)
            )
        } else {
            binding.atDisposalLabel.visibility = View.GONE
        }
    }

    private fun onOnrampClicked() {
        val intent = Intent(this, CcdOnrampSitesActivity::class.java)
        intent.putExtras(
            CcdOnrampSitesActivity.getBundle(
                accountAddress = viewModelAccountDetails.account.address,
            )
        )
        startActivity(intent)
    }

    private fun onSendFundsClicked() {
        val intent = Intent(this, SendTokenActivity::class.java)
        intent.putExtra(SendTokenActivity.ACCOUNT, viewModelAccountDetails.account)
        intent.putExtra(
            SendTokenActivity.TOKEN,
            Token.ccd(viewModelAccountDetails.account)
        )
        intent.putExtra(SendTokenActivity.PARENT_ACTIVITY, this::class.java.canonicalName)

        startActivityForResultAndHistoryCheck(intent)
    }

    private fun onAddressClicked() {
        val intent = Intent(this, AccountQRCodeActivity::class.java)
        intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
        startActivity(intent)
    }

    private fun onEarnClicked() {
        gotoEarn(
            this,
            viewModelAccountDetails.account,
            viewModelAccountDetails.hasPendingDelegationTransactions,
            viewModelAccountDetails.hasPendingBakingTransactions
        )
    }

    private fun onActivityClicked() {
        val intent = Intent(this, AccountDetailsTransfersActivity::class.java)
        intent.putExtra(
            AccountDetailsTransfersActivity.EXTRA_ACCOUNT,
            viewModelAccountDetails.account
        )
        startActivity(intent)
    }

    private fun showTokenDetailsDialog(token: Token) {
        val intent = Intent(this, TokenDetailsActivity::class.java)
        intent.putExtra(TokenDetailsActivity.ACCOUNT, viewModelAccountDetails.account)
        intent.putExtra(TokenDetailsActivity.TOKEN, token)
        showTokenDetails.launch(intent)
    }

    private fun setupOnrampBanner(active: Boolean) {
        binding.onrampBanner.root.setOnClickListener {
            if (active)
                onOnrampClicked()
            else
                showUnlockFeatureDialog()
        }
    }

    private val showTokenDetails =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val isChanged =
                    it.data?.getBooleanExtra(TokenDetailsActivity.CHANGED, false) == true
                if (isChanged) {
                    viewModelTokens.updateWithSelectedTokensDone.postValue(true)
                }
            }
        }
    //endregion
}
