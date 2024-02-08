package com.concordium.wallet.ui.account.accountdetails

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAccountDetailsBinding
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenActivity
import com.concordium.wallet.ui.cis2.TokenDetailsActivity
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EarnDelegateImpl
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsActivity
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

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_SHIELDED = "EXTRA_SHIELDED"
        const val RESULT_RETRY_ACCOUNT_CREATION = 2
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.extras?.getSerializable(EXTRA_ACCOUNT) as Account
        val isShielded = intent.extras?.getBoolean(EXTRA_SHIELDED)
        initializeViewModel()
        viewModelAccountDetails.initialize(account, isShielded ?: false)
        initializeViewModelTokens()
        initViews()
        hideActionBarBack(isVisible = true)
        hideSettings(isVisible = true) {
            goToAccountSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModelAccountDetails.populateTransferList()
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
        viewModelAccountDetails.totalBalanceLiveData.observe(this) { totalBalance ->
            if (viewModelAccountDetails.isShielded && totalBalance.second) {
                showAuthentication(
                    activity = this,
                    onCanceled = ::finish,
                    onAuthenticated = viewModelAccountDetails::continueWithPassword
                )
            } else {
                showTotalBalance(totalBalance.first)
            }
        }

        viewModelAccountDetails.selectedTransactionForDecrytionLiveData.observe(
            this
        ) {
            showAuthentication(
                activity = this,
                onCanceled = ::finish,
                onAuthenticated = viewModelAccountDetails::continueWithPassword
            )
        }

        viewModelAccountDetails.transferListLiveData.observe(this) {
            viewModelAccountDetails.checkForUndecryptedAmounts()
        }

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

        binding.sendFundsBtn.setOnClickListener {
            onSendFundsClicked()
        }
        binding.sendFundsBtn.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                if (viewModelAccountDetails.isShielded)
                    R.drawable.cryptox_ico_send_shielded
                else
                    R.drawable.cryptox_ico_share
            )
        )

        binding.addressBtn.setOnClickListener {
            onAddressClicked()
        }

        binding.earnBtn.setOnClickListener {
            gotoEarn(
                this,
                viewModelAccountDetails.account,
                viewModelAccountDetails.hasPendingDelegationTransactions,
                viewModelAccountDetails.hasPendingBakingTransactions
            )
        }
        binding.earnBtn.isVisible = !viewModelAccountDetails.isShielded
        binding.earnBtnDivider.isVisible = !viewModelAccountDetails.isShielded

        binding.shieldFundsBtn.setOnClickListener {
            onShieldFundsClicked()
        }
        binding.shieldFundsBtn.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                if (viewModelAccountDetails.isShielded)
                    R.drawable.cryptox_ico_unshield
                else
                    R.drawable.cryptox_ico_shielded
            )
        )
        binding.shieldFundsBtn.contentDescription =
            if (viewModelAccountDetails.isShielded)
                resources.getText(R.string.account_details_unshield)
            else
                resources.getText(R.string.account_details_shield)
        binding.shieldFundsBtn.tooltipText = binding.shieldFundsBtn.contentDescription

        initTabs()
    }

    private fun initTitle() {
        setActionBarTitle(
            getString(
                if (viewModelAccountDetails.isShielded) R.string.account_details_title_shielded_balance else R.string.account_details_title_regular_balance,
                viewModelAccountDetails.account.getAccountName()
            )
        )
    }

    private fun setFinalizedMode() {
        binding.shieldFundsBtn.isEnabled = !viewModelAccountDetails.account.readOnly
        binding.sendFundsBtn.isEnabled = !viewModelAccountDetails.account.readOnly
        binding.addressBtn.isEnabled = true
        binding.earnBtn.isEnabled = !viewModelAccountDetails.account.readOnly
        binding.walletInfoCard.readonlyDesc.visibility =
            if (viewModelAccountDetails.account.readOnly) View.VISIBLE else View.GONE

        if (viewModelAccountDetails.isShielded) {
            binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.visibility = View.GONE
            binding.walletInfoCard.disposalBlock.visibility = View.GONE
            binding.walletInfoCard.divider.visibility = View.GONE
        } else {
            binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.visibility = View.VISIBLE
            binding.walletInfoCard.disposalBlock.visibility = View.VISIBLE
            binding.walletInfoCard.divider.visibility = View.VISIBLE
            if (viewModelAccountDetails.account.isBaker()) {
                binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.visibility = View.VISIBLE
                binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.text =
                    viewModelAccountDetails.account.bakerId.toString()
                binding.walletInfoCard.bakerIdLabel.visibility = View.VISIBLE
            } else {
                binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.visibility = View.GONE
                binding.walletInfoCard.bakerIdLabel.visibility = View.GONE
            }

            val totalStaked = viewModelAccountDetails.account.totalStaked
            if (totalStaked.signum() > 0) {
                binding.walletInfoCard.stakedLabel.visibility = View.VISIBLE
                binding.walletInfoCard.accountsOverviewTotalDetailsStaked.visibility = View.VISIBLE
                binding.walletInfoCard.accountsOverviewTotalDetailsStaked.text =
                    CurrencyUtil.formatGTU(viewModelAccountDetails.account.totalStaked)
            } else {
                binding.walletInfoCard.stakedLabel.visibility = View.GONE
                binding.walletInfoCard.accountsOverviewTotalDetailsStaked.visibility = View.GONE
            }

            if (viewModelAccountDetails.account.isDelegating()) {
                binding.walletInfoCard.delegatingLabel.visibility = View.VISIBLE
                binding.walletInfoCard.accountsOverviewTotalDetailsDelegating.visibility =
                    View.VISIBLE
                binding.walletInfoCard.accountsOverviewTotalDetailsDelegating.text =
                    CurrencyUtil.formatGTU(
                        viewModelAccountDetails.account.accountDelegation?.stakedAmount ?: BigInteger.ZERO
                    )
            } else {
                binding.walletInfoCard.delegatingLabel.visibility = View.GONE
                binding.walletInfoCard.accountsOverviewTotalDetailsDelegating.visibility = View.GONE
            }
        }
    }

    private fun setErrorMode() {
        setPendingMode()
        binding.accountRetryButton.visibility = View.VISIBLE
        binding.accountRemoveButton.visibility = View.VISIBLE
    }

    private fun setPendingMode() {
        binding.sendFundsBtn.isEnabled = false
        binding.shieldFundsBtn.isEnabled = false
        binding.addressBtn.isEnabled = false
        binding.earnBtn.isEnabled = false
    }

    private fun initTabs() {
        val adapter =
            AccountDetailsPagerAdapter(
                supportFragmentManager,
                viewModelAccountDetails.account,
                this
            )
        binding.accountDetailsPager.adapter = adapter
        binding.accountDetailsTablayout.setupWithViewPager(binding.accountDetailsPager)

        // Make the pager height match the container height except the buttons
        // to enable full hide of the header card by scrolling.
        var handledContainerHeight = -1
        binding.scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val containerHeight = binding.scrollView.measuredHeight
            val buttonsHeight = binding.buttonsBlock.measuredHeight
            val buttonsMargin =
                (binding.accountDetailsPager.layoutParams as MarginLayoutParams).topMargin

            if (handledContainerHeight != containerHeight) {
                handledContainerHeight = containerHeight

                binding.accountDetailsPager.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = containerHeight - buttonsHeight - buttonsMargin
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
            putExtra(AccountSettingsActivity.EXTRA_SHIELDED, viewModelAccountDetails.isShielded)
        })
    }

    private fun showTotalBalance(totalBalance: BigInteger) {
        binding.walletInfoCard.totalBalanceTextview.text = CurrencyUtil.formatGTU(totalBalance)
        binding.walletInfoCard.accountsOverviewTotalDetailsDisposal.text = CurrencyUtil.formatGTU(
            viewModelAccountDetails.account.getAtDisposalWithoutStakedOrScheduled(totalBalance),
            true
        )
    }

    private fun onSendFundsClicked() {
        val intent: Intent
        if (viewModelAccountDetails.isShielded) {
            intent = Intent(this, SendFundsActivity::class.java)
            intent.putExtra(SendFundsActivity.EXTRA_SHIELDED, viewModelAccountDetails.isShielded)
            intent.putExtra(SendFundsActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
        } else {
            intent = Intent(this, SendTokenActivity::class.java)
            intent.putExtra(SendTokenActivity.ACCOUNT, viewModelAccountDetails.account)
            intent.putExtra(
                SendTokenActivity.TOKEN,
                Token.ccd(viewModelAccountDetails.account)
            )
            intent.putExtra(SendTokenActivity.PARENT_ACTIVITY, this::class.java.canonicalName)
        }

        startActivityForResultAndHistoryCheck(intent)
    }

    private fun onShieldFundsClicked() {
        val intent = Intent(this, SendFundsActivity::class.java)
        intent.putExtra(SendFundsActivity.EXTRA_SHIELDED, viewModelAccountDetails.isShielded)
        intent.putExtra(SendFundsActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
        intent.putExtra(
            SendFundsActivity.EXTRA_RECIPIENT,
            Recipient(
                viewModelAccountDetails.account.id,
                viewModelAccountDetails.account.name,
                viewModelAccountDetails.account.address
            )
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun onAddressClicked() {
        val intent = Intent(this, AccountQRCodeActivity::class.java)
        intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
        startActivity(intent)
    }

    private fun showTokenDetailsDialog(token: Token) {
        val intent = Intent(this, TokenDetailsActivity::class.java)
        intent.putExtra(TokenDetailsActivity.ACCOUNT, viewModelAccountDetails.account)
        intent.putExtra(TokenDetailsActivity.TOKEN, token)
        showTokenDetails.launch(intent)
    }

    private val showTokenDetails =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.getBooleanExtra(TokenDetailsActivity.DELETED, false)?.let { isDeleted ->
                    if (isDeleted) {
                        viewModelTokens.updateWithSelectedTokensDone.postValue(true)
                    }
                }
            }
        }
    //endregion
}
