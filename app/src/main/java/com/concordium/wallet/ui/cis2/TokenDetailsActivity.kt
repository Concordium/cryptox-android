package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityTokenDetailsBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.account.accountdetails.transfers.AccountDetailsTransfersActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EarnDelegateImpl
import com.concordium.wallet.ui.onramp.CcdOnrampSitesActivity
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PrettyPrint.asJsonString
import com.concordium.wallet.util.getSerializable
import java.math.BigInteger

class TokenDetailsActivity : BaseActivity(R.layout.activity_token_details),
    EarnDelegate by EarnDelegateImpl() {
    private lateinit var binding: ActivityTokenDetailsBinding
    private val viewModel: TokensViewModel by viewModels()

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
        viewModel.tokenData.account =
            requireNotNull(intent.getSerializable(ACCOUNT, Account::class.java)) {
                "Missing account extra"
            }
        viewModel.tokenData.selectedToken =
            requireNotNull(intent.getSerializable(TOKEN, Token::class.java)) {
                "Missing token extra"
            }
        viewModel.tokenData.hasPendingDelegationTransactions =
            requireNotNull(intent.getBooleanExtra(PENDING_DELEGATION, false)) {
                "Missing delegation extra"
            }
        viewModel.tokenData.hasPendingValidationTransactions =
            requireNotNull(intent.getBooleanExtra(PENDING_VALIDATION, false)) {
                "Missing delegation extra"
            }

        Log.d("TOKEN : ${viewModel.tokenData}")
        Log.d("ACCOUNT : ${viewModel.tokenData.account}")

        setActionBarTitle(getString(R.string.cis_token_details_balance))
        hideActionBarBack(true)

        binding.ccdActionButtons.onrampBtn.setOnClickListener {
            onOnrampClicked()
        }
        binding.ccdActionButtons.sendFundsBtn.isEnabled =
            viewModel.tokenData.account.let {
                it != null && !it.readOnly && it.transactionStatus == TransactionStatus.FINALIZED
            }
        binding.ccdActionButtons.sendFundsBtn.setOnClickListener {
            onSendClicked()
        }
        binding.ccdActionButtons.receiveBtn.setOnClickListener {
            onReceiveClicked()
        }

        binding.ccdActionButtons.earnBtnNotice.isVisible =
            viewModel.tokenData.account.let {
                it != null && (it.isBakerSuspended || it.isBakerPrimedForSuspension || it.isDelegationBakerSuspended)
            }
        binding.ccdActionButtons.earnBtn.setOnClickListener {
            onEarnClicked()
        }

        binding.ccdActionButtons.activityBtn.setOnClickListener {
            onActivityClicked()
        }

        binding.cisTokenActionButtons.sendFundsBtn.isEnabled =
            viewModel.tokenData.account.let {
                it != null && !it.readOnly && it.transactionStatus == TransactionStatus.FINALIZED
            }
        binding.cisTokenActionButtons.sendFundsBtn.setOnClickListener {
            onSendClicked()
        }
        binding.cisTokenActionButtons.receiveBtn.setOnClickListener {
            onReceiveClicked()
        }
        binding.includeAbout.hideToken.setOnClickListener {
            showDeleteDialog()
        }

        viewModel.tokenData.selectedToken?.let { token ->
            setContractIndexAndSubIndex(token)
            setTokenId(token.token)
            setBalances(token)
            token.metadata?.let { tokenMetadata ->
                setNameAndIcon(token.isCcd, token.isUnique, tokenMetadata)
                setOwnership(token, tokenMetadata)
                setDescription(token.isCcd, tokenMetadata)
                setTicker(tokenMetadata)
                setDecimals(token)
                setRawMetadataButton(token.isCcd, tokenMetadata)
            }
            setHideButton(token.isCcd)
            if (token.isNewlyReceived) {
                handleNewlyReceivedToken(token)
            }
        }
    }

    private fun handleNewlyReceivedToken(token: Token) {
        viewModel.unmarkNewlyReceivedSelectedToken()
        showNewlyReceivedNotice(
            tokenName = token.name ?: token.symbol
        )
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(CHANGED, true)
        )
    }

    private fun showDeleteDialog() {
        viewModel.tokenData.selectedToken?.symbol?.let {
            HidingTokenDialog.newInstance(
                HidingTokenDialog.getBundle(tokenName = it)
            ).showSingle(supportFragmentManager, HidingTokenDialog.TAG)
        }
    }

    private fun setBalances(token: Token) {
        binding.walletInfoCard.totalBalanceTextview.text = if (token.isCcd) {
            CurrencyUtil.formatGTU(viewModel.tokenData.account?.balance!!, token) +
                    " ${token.symbol}"
        } else {
            CurrencyUtil.formatGTU(token.balance, token) +
                    " ${token.symbol}"
        }

        viewModel.tokenData.account?.readOnly?.let {
            binding.ccdActionButtons.sendFundsBtn.isEnabled = !it
            binding.cisTokenActionButtons.sendFundsBtn.isEnabled = !it
            binding.ccdActionButtons.earnBtn.isEnabled = !it
            binding.walletInfoCard.readonlyDesc.visibility = if (it) View.VISIBLE else View.GONE
        }
        binding.apply {
            ccdActionButtons.onrampBtn.isEnabled = true
            ccdActionButtons.receiveBtn.isEnabled = true
            cisTokenActionButtons.receiveBtn.isEnabled = true
            walletInfoCard.accountsOverviewTotalDetailsBakerId.visibility = View.VISIBLE
            walletInfoCard.disposalBlock.visibility = View.VISIBLE
        }

        if (token.isCcd) {
            binding.ccdActionButtons.root.visibility = View.VISIBLE
            binding.cisTokenActionButtons.root.visibility = View.GONE
            binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.text =
                viewModel.tokenData.account?.baker?.bakerId?.toString()

            viewModel.tokenData.account?.let { account ->
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
                }
                account.hasCooldowns().also { hasCooldown ->
                    binding.walletInfoCard.cooldownLabel.isVisible = hasCooldown
                    binding.walletInfoCard.accountsOverviewTotalDetailsCooldown.isVisible =
                        hasCooldown
                }
                if (account.balance != account.balanceAtDisposal) {
                    binding.walletInfoCard.atDisposalLabel.visibility = View.VISIBLE
                    binding.walletInfoCard.accountsOverviewTotalDetailsDisposal.visibility = View.VISIBLE
                }
            }

            viewModel.tokenData.account?.stakedAmount?.let {
                binding.walletInfoCard.accountsOverviewTotalDetailsStaked.text = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it)
                )
            }

            viewModel.tokenData.account?.balanceAtDisposal?.let {
                binding.walletInfoCard.accountsOverviewTotalDetailsDisposal.text = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it)
                )
            }

            viewModel.tokenData.account?.delegatedAmount?.let {
                binding.walletInfoCard.accountsOverviewTotalDetailsDelegating.text = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it)
                )
            }
            viewModel.tokenData.account?.cooldownAmount?.let {
                binding.walletInfoCard.accountsOverviewTotalDetailsCooldown.text = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it)
                )
            }
        } else {
            binding.apply {
                walletInfoCard.disposalBlock.isVisible = false
                ccdActionButtons.root.visibility = View.GONE
                cisTokenActionButtons.root.visibility = View.VISIBLE
            }
        }
    }

    private fun setRawMetadataButton(isCCD: Boolean, tokenMetadata: TokenMetadata) {
        binding.includeAbout.rawMetadataBtn.setOnClickListener {
            RawMetadataDialog.newInstance(
                RawMetadataDialog.setBundle(
                    rawMetadata = if (isCCD) getString(R.string.ccd_details_description)
                    else tokenMetadata.asJsonString()
                )
            ).showSingle(supportFragmentManager, RawMetadataDialog.TAG)
        }
    }

    private fun setTokenId(tokenId: String) {
        if (tokenId.isNotBlank()) {
            binding.includeAbout.tokenIdHolder.visibility = View.VISIBLE
            binding.includeAbout.tokenId.text = tokenId
        }
    }

    private fun setDescription(isCCD: Boolean, tokenMetadata: TokenMetadata) {
        if (isCCD) {
            binding.includeAbout.descriptionHolder.visibility = View.VISIBLE
            binding.includeAbout.description.text = getString(R.string.ccd_details_description)
        }
        if (!tokenMetadata.description.isNullOrBlank()) {
            binding.includeAbout.descriptionHolder.visibility = View.VISIBLE
            binding.includeAbout.description.text = tokenMetadata.description
        }
    }

    private fun setOwnership(token: Token, tokenMetadata: TokenMetadata) {
        if (tokenMetadata.unique == true) {
            binding.includeAbout.ownershipHolder.visibility = View.VISIBLE
            binding.includeAbout.ownership.text =
                if (token.balance != BigInteger.ZERO)
                    getString(R.string.cis_owned)
                else
                    getString(R.string.cis_not_owned)
        }
    }

    private fun setNameAndIcon(isCCD: Boolean, isUnique: Boolean, tokenMetadata: TokenMetadata) {
        val name = if (isCCD) getString(R.string.account_details_ccd_token) else tokenMetadata.name
        val thumbnail = tokenMetadata.thumbnail?.url
        val iconView = binding.includeAbout.icon

        binding.includeAbout.nameAndIconHolder.visibility = View.VISIBLE
        binding.includeAbout.name.text = name

        when {
            !thumbnail.isNullOrBlank() -> {
                loadImage(iconView, thumbnail)
                if (isUnique) {
                    binding.includeAbout.nftIcon.visibility = View.VISIBLE
                    tokenMetadata.display?.url?.let { loadImage(binding.includeAbout.nftIcon, it) }
                }
            }
            !thumbnail.isNullOrBlank() -> loadImage(iconView, thumbnail)
            isCCD -> iconView.setImageResource(R.drawable.mw24_ic_ccd)
            else -> iconView.setImageResource(R.drawable.mw24_ic_token_placeholder)
        }
    }

    private fun loadImage(view: AppCompatImageView, url: String) {
        Glide.with(view.context)
            .load(url)
            .placeholder(R.drawable.mw24_ic_token_placeholder)
            .fitCenter()
            .into(view)
    }

    private fun setContractIndexAndSubIndex(token: Token) {
        val tokenIndex = token.contractIndex
        if (tokenIndex.isNotBlank()) {
            binding.includeAbout.contractIndexHolder.visibility = View.VISIBLE
            binding.includeAbout.contractIndex.text = token.contractIndex
            if (token.subIndex.isNotBlank()) {
                val combinedInfo = "${tokenIndex}, ${token.subIndex}"
                binding.includeAbout.contractIndex.text = combinedInfo
            } else {
                binding.includeAbout.contractIndex.text = tokenIndex
            }
        }
    }

    private fun setTicker(tokenMetadata: TokenMetadata) {
        if (!tokenMetadata.symbol.isNullOrBlank()) {
            binding.includeAbout.tokenHolder.visibility = View.VISIBLE
            binding.includeAbout.token.text = tokenMetadata.symbol
        }
    }

    private fun setDecimals(token: Token) {
        if (!token.isUnique) {
            binding.includeAbout.decimalsHolder.visibility = View.VISIBLE
            binding.includeAbout.decimals.text = getString(
                R.string.account_token_details_decimals,
                token.decimals.toString()
            )
        }
    }

    private fun setHideButton(isCCD: Boolean) {
        binding.includeAbout.hideToken.isVisible = !isCCD
    }

    private fun showNewlyReceivedNotice(tokenName: String) {
        NewlyReceivedTokenNoticeDialog.newInstance(
            NewlyReceivedTokenNoticeDialog.getBundle(
                tokenName = tokenName,
            )
        ).showSingle(supportFragmentManager, NewlyReceivedTokenNoticeDialog.TAG)
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }

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
                    Activity.RESULT_OK,
                    Intent().putExtra(CHANGED, true)
                )
                finish()
            }
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun onOnrampClicked() {
        val intent = Intent(this, CcdOnrampSitesActivity::class.java)
        intent.putExtras(CcdOnrampSitesActivity.getBundle(viewModel.tokenData.account?.address))
        startActivity(intent)
    }

    private fun onSendClicked() {
        val intent = Intent(this, SendTokenActivity::class.java)
        intent.putExtra(SendTokenActivity.ACCOUNT, viewModel.tokenData.account)
        intent.putExtra(SendTokenActivity.TOKEN, viewModel.tokenData.selectedToken)
        intent.putExtra(
            SendTokenActivity.PARENT_ACTIVITY,
            MainActivity::class.java.canonicalName
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun onReceiveClicked() {
        val intent = Intent(this, AccountQRCodeActivity::class.java)
        intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, viewModel.tokenData.account)
        startActivity(intent)
    }

    private fun onEarnClicked() {
        viewModel.tokenData.account?.let {
            gotoEarn(
                this,
                it,
                viewModel.tokenData.hasPendingDelegationTransactions,
                viewModel.tokenData.hasPendingValidationTransactions
            )
        }
    }

    private fun onActivityClicked() {
        viewModel.tokenData.selectedToken?.let {
            val intent = Intent(this, AccountDetailsTransfersActivity::class.java)
            intent.putExtra(
                AccountDetailsTransfersActivity.EXTRA_ACCOUNT,
                viewModel.tokenData.account
            )
            startActivity(intent)
        }
    }
}
