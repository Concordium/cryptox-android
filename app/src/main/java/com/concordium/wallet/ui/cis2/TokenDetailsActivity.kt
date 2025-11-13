package com.concordium.wallet.ui.cis2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityTokenDetailsBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.account.accountdetails.AccountReleaseScheduleActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.getSerializable
import java.math.BigInteger

class TokenDetailsActivity : BaseActivity(R.layout.activity_token_details) {
    private lateinit var binding: ActivityTokenDetailsBinding

    private val viewModel: TokenDetailsViewModel by viewModels()

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val TOKEN = "TOKEN"
        const val CHANGED = "CHANGED"
        const val PENDING_DELEGATION = "PENDING_DELEGATION"
        const val PENDING_VALIDATION = "PENDING_VALIDATION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenDetailsBinding.bind(findViewById(R.id.root_layout))
        initViews()
        initObservers()
    }

    private fun initViews() {
        viewModel.tokenDetailsData.account =
            requireNotNull(intent.getSerializable(ACCOUNT, Account::class.java)) {
                "Missing account extra"
            }

        @Suppress("DEPRECATION")
        val token = intent.getSerializableExtra(TOKEN) as Token
        viewModel.tokenDetailsData.selectedToken = token

        viewModel.tokenDetailsData.hasPendingDelegationTransactions =
            requireNotNull(intent.getBooleanExtra(PENDING_DELEGATION, false)) {
                "Missing delegation extra"
            }

        viewModel.tokenDetailsData.hasPendingValidationTransactions =
            requireNotNull(intent.getBooleanExtra(PENDING_VALIDATION, false)) {
                "Missing delegation extra"
            }

        Log.d("TOKEN : ${viewModel.tokenDetailsData}")
        Log.d("ACCOUNT : ${viewModel.tokenDetailsData.account}")

        setActionBarTitle(getString(R.string.cis_token_details_balance))
        hideActionBarBack(true)

        setBalances(token)

        TokenDetailsView(
            binding = binding.detailsLayout,
            fragmentManager = supportFragmentManager,
        )
            .showTokenDetails(
                token = token,
                isHideVisible = true,
                onHideClicked = ::showDeleteDialog,
                onReleaseScheduleClicked = {
                    gotoAccountReleaseSchedule(viewModel.tokenDetailsData.account!!)
                }
            )

        if (token.isNewlyReceived) {
            handleNewlyReceivedToken(token)
        }
    }

    private fun handleNewlyReceivedToken(token: Token) {
        viewModel.unmarkNewlyReceivedSelectedToken()
        showNewlyReceivedNotice(tokenName = token.symbol)
        setResult(
            RESULT_OK,
            Intent().putExtra(CHANGED, true)
        )
    }

    private fun showDeleteDialog() {
        HidingTokenDialog.newInstance(
            HidingTokenDialog.getBundle(
                tokenName =
                    viewModel.tokenDetailsData.selectedToken?.symbol!!,
            )
        ).showSingle(supportFragmentManager, HidingTokenDialog.TAG)
    }

    @SuppressLint("SetTextI18n")
    private fun setBalances(token: Token) {
        val balance: BigInteger =
            if (token is CCDToken)
                viewModel.tokenDetailsData.account?.balance!!
            else
                token.balance

        binding.walletInfoCard.totalBalanceTextview.text =
            CurrencyUtil.formatGTU(balance, token) + " ${token.symbol}"

        viewModel.tokenDetailsData.account?.readOnly?.let {
            binding.walletInfoCard.readonlyDesc.visibility = if (it) View.VISIBLE else View.GONE
        }
        true
        binding.apply {
            walletInfoCard.accountsOverviewTotalDetailsBakerId.visibility = View.VISIBLE
            walletInfoCard.disposalBlock.visibility = View.VISIBLE
        }

        if (token is CCDToken) {
            binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.text =
                viewModel.tokenDetailsData.account?.baker?.bakerId?.toString()

            viewModel.tokenDetailsData.account?.let { account ->
                account.isBaking().also { isBaking ->
                    binding.walletInfoCard.stakedLabel.isVisible = isBaking
                    binding.walletInfoCard.accountsOverviewTotalDetailsStaked.isVisible =
                        isBaking
                    binding.walletInfoCard.bakerIdLabel.isVisible = isBaking
                    binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.isVisible =
                        isBaking
                }
                account.isDelegating().also { delegating ->
                    binding.walletInfoCard.delegatingLabel.isVisible = delegating
                    binding.walletInfoCard.accountsOverviewTotalDetailsDelegating.isVisible =
                        delegating
                    binding.earningCard.root.isVisible = delegating
                }
                account.hasCooldowns().also { hasCooldown ->
                    binding.walletInfoCard.cooldownLabel.isVisible = hasCooldown
                    binding.walletInfoCard.accountsOverviewTotalDetailsCooldown.isVisible =
                        hasCooldown
                }
                if (account.balance != account.balanceAtDisposal) {
                    binding.walletInfoCard.atDisposalLabel.visibility = View.VISIBLE
                    binding.walletInfoCard.accountsOverviewTotalDetailsDisposal.visibility =
                        View.VISIBLE
                }
            }

            viewModel.tokenDetailsData.account?.stakedAmount?.let {
                binding.walletInfoCard.accountsOverviewTotalDetailsStaked.text = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it)
                )
            }

            viewModel.tokenDetailsData.account?.balanceAtDisposal?.let {
                binding.walletInfoCard.accountsOverviewTotalDetailsDisposal.text = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it)
                )
            }

            viewModel.tokenDetailsData.account?.delegatedAmount?.let {
                val amount = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it)
                )
                binding.walletInfoCard.accountsOverviewTotalDetailsDelegating.text = amount
                binding.earningCard.stakedBalance.text = amount
            }
            viewModel.tokenDetailsData.account?.cooldownAmount?.let {
                binding.walletInfoCard.accountsOverviewTotalDetailsCooldown.text = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it)
                )
            }
            binding.earningCard.stakedMode.text =
                if (viewModel.tokenDetailsData.account?.delegation?.delegationTarget?.bakerId != null)
                    getString(R.string.accounts_overview_staked_mode_pool)
                else
                    getString(R.string.accounts_overview_staked_mode_passive)
            binding.earningCard.root.setOnClickListener { gotoEarn() }
        } else {
            binding.apply {
                walletInfoCard.disposalBlock.isVisible = false
                earningCard.root.isVisible = false
            }
        }
    }

    private fun showNewlyReceivedNotice(tokenName: String) {
        NewlyReceivedTokenNoticeDialog.newInstance(
            NewlyReceivedTokenNoticeDialog.getBundle(
                tokenName = tokenName,
            )
        ).showSingle(supportFragmentManager, NewlyReceivedTokenNoticeDialog.TAG)
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            NewlyReceivedTokenNoticeDialog.ACTION_REQUEST,
            this,
        ) { _, bundle ->
            val isKeepingToken = NewlyReceivedTokenNoticeDialog.getResult(bundle)
            if (!isKeepingToken) {
                showDeleteDialog()
            }
        }

        supportFragmentManager.setFragmentResultListener(
            HidingTokenDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            val isHidingToken = HidingTokenDialog.getResult(bundle)
            if (isHidingToken) {
                viewModel.deleteSelectedToken()
                setResult(
                    RESULT_OK,
                    Intent().putExtra(CHANGED, true)
                )
                finish()
            }
        }
    }

    private fun gotoAccountReleaseSchedule(account: Account) {
        val intent = Intent(this, AccountReleaseScheduleActivity::class.java)
        intent.putExtra(AccountReleaseScheduleActivity.EXTRA_ACCOUNT, account)
        startActivity(intent)
    }

    private fun gotoEarn() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_GOTO_EARN, true)
        startActivity(intent)
        finishAffinity()
    }
}
