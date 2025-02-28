package com.concordium.wallet.ui.bakerdelegation.baker

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.CONFIGURE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityBakerRegistrationConfirmationBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.dialog.baker.BakerErrorDialog
import com.concordium.wallet.ui.bakerdelegation.dialog.baker.BakerNoticeDialog
import com.concordium.wallet.util.UnitConvertUtil
import java.math.BigInteger
import java.text.DecimalFormat

class BakerRegistrationConfirmationActivity : BaseDelegationBakerActivity(
    R.layout.activity_baker_registration_confirmation,
    R.string.baker_registration_confirmation_title
) {
    private var receiptMode = false

    private lateinit var binding: ActivityBakerRegistrationConfirmationBinding

    /**
     * Formats values in range [[0.0; 1.0]].
     */
    private val percentFormat = (DecimalFormat.getPercentInstance() as DecimalFormat).apply {
        minimumFractionDigits = PERCENT_FORMAT_DECIMALS
        maximumFractionDigits = PERCENT_FORMAT_DECIMALS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationConfirmationBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
        initObservers()
    }

    override fun onBackPressed() {
        if (!receiptMode)
            super.onBackPressed()
    }

    override fun initViews() {
        super.initViews()
        showWaiting(binding.includeProgress.progressLayout, true)
        loadFee()
    }

    private fun loadFee() {
        viewModel.transactionFeeLiveData.observe(this) { response ->
            response?.first?.let {
                showWaiting(binding.includeProgress.progressLayout, false)
                updateViews()
                binding.estimatedTransactionFee.text = getString(
                    R.string.delegation_register_delegation_amount_estimated_transaction_fee,
                    CurrencyUtil.formatGTU(it)
                )
            }
        }
        viewModel.loadTransactionFee(true)
    }

    private fun updateViews() {

        binding.submitBakerTransaction.text =
            getString(R.string.baker_registration_confirmation_submit)
        binding.accountToBakeFrom.text =
            viewModel.bakerDelegationData.account.name
                .plus("\n\n")
                .plus(viewModel.bakerDelegationData.account.address)
        binding.estimatedTransactionFee.visibility = View.VISIBLE

        when (viewModel.bakerDelegationData.type) {
            REGISTER_BAKER -> {
                updateViewsRegisterBaker()
            }

            UPDATE_BAKER_KEYS -> {
                updateViewsUpdateBakerKeys()
            }

            UPDATE_BAKER_POOL -> {
                updateViewsUpdateBakerPool()
            }

            UPDATE_BAKER_STAKE -> {
                updateViewsUpdateBakerStake()
            }

            REMOVE_BAKER -> {
                updateViewsRemoveBaker()
            }

            CONFIGURE_BAKER -> {
                if (viewModel.bakerDelegationData.toSetBakerSuspended == false) {
                    updateViewsResumeBaker()
                } else if (viewModel.bakerDelegationData.toSetBakerSuspended == true){
                    updateViewsSuspendBaker()
                }
            }
        }

        binding.submitBakerTransaction.setOnClickListener {
            onContinueClicked()
        }

        binding.submitBakerFinish.setOnClickListener {
            showNotice()
        }

        initializeShowAuthenticationLiveData()
        initializeWaitingLiveData(binding.includeProgress.progressLayout)

        viewModel.transactionSuccessLiveData.observe(this) { waiting ->
            waiting?.let {
                showPageAsReceipt()
            }
        }
    }

    private fun updateViewsRegisterBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_title)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_register_confirmation_receipt_title)
        showAmount()
        showRewards()
        showPoolStatus()
        showCommissionRates()
        showMetaUrl()
        showKeys()
    }

    private fun updateViewsUpdateBakerKeys() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_keys_title)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_update_keys_transaction_title)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_update_affected_account)
        showKeys()
        showCommissionRates()
    }

    private fun updateViewsUpdateBakerPool() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_pool_title)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_update_pool_transaction_title)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_update_affected_account)
        showPoolStatus()
        showCommissionRates()
        showMetaUrl()
    }

    private fun updateViewsUpdateBakerStake() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_stake_title)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_update_stake_transaction_title)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_update_stake_update)
        showAmount()
        showRewards()
        showCommissionRates()
    }

    private fun updateViewsRemoveBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_remove_title)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_remove_transaction)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_remove_account_to_stop)
        hideCommissionRates()
    }

    private fun updateViewsResumeBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_resume_title)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_resume_transaction)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_resume_account_to_stop)
        hideCommissionRates()
    }

    private fun updateViewsSuspendBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_suspend_title)
        binding.delegationTransactionTitle.text =
            getString(R.string.baker_registration_confirmation_suspend_transaction)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_suspend_account_to_stop)
        hideCommissionRates()
    }

    private fun showAmount() {
        if (viewModel.stakedAmountHasChanged()) {
            binding.delegationAmountConfirmationTitle.visibility = View.VISIBLE
            binding.bakerAmountConfirmation.visibility = View.VISIBLE
            binding.bakerAmountConfirmation.text = CurrencyUtil.formatGTU(
                viewModel.bakerDelegationData.amount ?: BigInteger.ZERO
            )
        }
    }

    private fun showRewards() {
        if (viewModel.restakeHasChanged()) {
            binding.rewardsWillBeTitle.visibility = View.VISIBLE
            binding.rewardsWillBe.visibility = View.VISIBLE
            binding.rewardsWillBe.text =
                if (viewModel.bakerDelegationData.restake) getString(R.string.baker_register_confirmation_receipt_added_to_delegation_amount) else getString(
                    R.string.baker_register_confirmation_receipt_at_disposal
                )
        }
    }

    private fun showPoolStatus() {
        if (viewModel.openStatusHasChanged()) {
            binding.poolStatusTitle.visibility = View.VISIBLE
            binding.poolStatus.visibility = View.VISIBLE
            binding.poolStatus.text =
                if (viewModel.isOpenBaker()) getString(R.string.baker_register_confirmation_receipt_pool_status_open) else getString(
                    R.string.baker_register_confirmation_receipt_pool_status_closed
                )
        }
    }

    private fun showCommissionRates() {
        if (viewModel.commissionRatesHasChanged().not()) {
            hideCommissionRates()
            return
        }

        binding.apply {
            if (viewModel.bakerDelegationData.transactionCommissionRate != null) {
                transactionFeeTitle.visibility = View.VISIBLE
                transactionFeeStatus.visibility = View.VISIBLE
                transactionFeeStatus.text =
                    percentFormat.format(viewModel.bakerDelegationData.transactionCommissionRate)
            } else {
                transactionFeeTitle.visibility = View.GONE
                transactionFeeStatus.visibility = View.GONE
            }

            if (viewModel.bakerDelegationData.bakingCommissionRate != null) {
                bakingTitle.visibility = View.VISIBLE
                bakingStatus.visibility = View.VISIBLE
                bakingStatus.text =
                    percentFormat.format(viewModel.bakerDelegationData.bakingCommissionRate)
            } else {
                bakingTitle.visibility = View.GONE
                bakingStatus.visibility = View.GONE
            }
        }
    }

    private fun hideCommissionRates() {
        binding.apply {
            transactionFeeTitle.visibility = View.GONE
            transactionFeeStatus.visibility = View.GONE
            bakingTitle.visibility = View.GONE
            bakingStatus.visibility = View.GONE
        }
    }

    private fun showKeys() {
        binding.electionVerifyKeyTitle.visibility = View.VISIBLE
        binding.electionVerifyKey.visibility = View.VISIBLE
        binding.signatureVerifyKeyTitle.visibility = View.VISIBLE
        binding.signatureVerifyKey.visibility = View.VISIBLE
        binding.aggregationVerifyKeyTitle.visibility = View.VISIBLE
        binding.aggregationVerifyKey.visibility = View.VISIBLE
        binding.electionVerifyKey.text = viewModel.bakerDelegationData.bakerKeys?.electionVerifyKey
        binding.signatureVerifyKey.text =
            viewModel.bakerDelegationData.bakerKeys?.signatureVerifyKey
        binding.aggregationVerifyKey.text =
            viewModel.bakerDelegationData.bakerKeys?.aggregationVerifyKey
    }

    private fun showMetaUrl() {
        if (viewModel.metadataUrlHasChanged()) {
            binding.metaDataUrlTitle.visibility = View.VISIBLE
            binding.metaDataUrl.visibility = View.VISIBLE
            if ((viewModel.bakerDelegationData.metadataUrl?.length
                    ?: 0) > 0
            ) binding.metaDataUrl.text = viewModel.bakerDelegationData.metadataUrl
            else binding.metaDataUrl.text =
                getString(R.string.baker_update_pool_settings_url_removed)
        }
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            BakerErrorDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            if (BakerErrorDialog.getResult(bundle)) {
                onContinueClicked()
            }
        }
    }

    private fun onContinueClicked() {
        if (viewModel.atDisposal() < (viewModel.bakerDelegationData.cost ?: BigInteger.ZERO)) {
            showNotEnoughFunds()
            return
        }
        viewModel.prepareTransaction()
    }

    private fun showPageAsReceipt() {
        receiptMode = true
        hideActionBarBack(isVisible = false)
        binding.submitBakerTransaction.visibility = View.GONE
        binding.submitBakerFinish.visibility = View.VISIBLE
        binding.includeTransactionSubmittedHeader.transactionSubmitted.visibility = View.VISIBLE
        viewModel.bakerDelegationData.submissionId?.let { submissionId ->
            binding.transactionHashView.isVisible = true
            binding.transactionHashView.transactionHash = submissionId
        }
    }

    private fun showNotice() {
        var noticeMessage = getString(R.string.baker_notice_message)

        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && (viewModel.bakerDelegationData.oldStakedAmount
                ?: BigInteger.ZERO) < (viewModel.bakerDelegationData.amount ?: BigInteger.ZERO)
        ) {
            noticeMessage = getString(R.string.baker_notice_message_update_increase)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && (viewModel.bakerDelegationData.oldStakedAmount
                ?: BigInteger.ZERO) > (viewModel.bakerDelegationData.amount ?: BigInteger.ZERO)
        ) {
            val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(
                viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0
            )
            noticeMessage = resources.getQuantityString(
                R.plurals.baker_notice_message_update_decrease, gracePeriod, gracePeriod
            )
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_STAKE && (viewModel.bakerDelegationData.oldStakedAmount
                ?: BigInteger.ZERO) == (viewModel.bakerDelegationData.amount ?: BigInteger.ZERO)
        ) {
            noticeMessage = getString(R.string.baker_notice_message_update_pool)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            noticeMessage = getString(R.string.baker_notice_message_update_pool)
        } else if (viewModel.bakerDelegationData.type == UPDATE_BAKER_KEYS) {
            noticeMessage = getString(R.string.baker_notice_message_update_keys)
        } else if (viewModel.bakerDelegationData.type == REMOVE_BAKER) {
            val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(
                viewModel.bakerDelegationData.chainParameters?.poolOwnerCooldown ?: 0
            )
            noticeMessage = resources.getQuantityString(
                R.plurals.baker_notice_message_remove, gracePeriod, gracePeriod
            )
        }

        BakerNoticeDialog.newInstance(
            BakerNoticeDialog.setBundle(noticeMessage)
        ).showSingle(supportFragmentManager, BakerNoticeDialog.TAG)
    }

    override fun errorLiveData(value: Int) {
        val messageFromWalletProxy = getString(value)

        BakerErrorDialog.newInstance(
            BakerErrorDialog.setBundle(
                getString(R.string.baker_register_transaction_failed, messageFromWalletProxy)
            )
        ).showSingle(supportFragmentManager, BakerErrorDialog.TAG)
    }

    override fun showWaiting(progressLayout: View, waiting: Boolean) {
        super.showWaiting(progressLayout, waiting)
        binding.submitBakerTransaction.isEnabled = !waiting
    }

    private companion object {
        private const val PERCENT_FORMAT_DECIMALS = 3
    }
}
