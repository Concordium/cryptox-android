package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.CONFIGURE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityBakerRegistrationConfirmationBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.dialog.baker.BakerErrorDialog
import com.concordium.wallet.ui.bakerdelegation.dialog.baker.BakerNoticeDialog
import com.concordium.wallet.ui.transaction.transactiondetails.TransactionDetailsActivity
import com.concordium.wallet.uicore.button.SliderButton
import com.concordium.wallet.util.UnitConvertUtil
import java.math.BigInteger
import java.text.DecimalFormat
import java.util.Locale

class BakerRegistrationConfirmationActivity : BaseDelegationBakerActivity(
    R.layout.activity_baker_registration_confirmation,
    R.string.baker_registration_confirmation_title
) {
    private var receiptMode = false

    private lateinit var binding: ActivityBakerRegistrationConfirmationBinding
    private lateinit var sliderButton: SliderButton

    /**
     * Formats values in range [[0.0; 1.0]].
     */
    private val percentFormat =
        (DecimalFormat.getPercentInstance(Locale.US) as DecimalFormat).apply {
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
        sliderButton = binding.submitBakerTransaction
        showWaiting(binding.includeProgress.progressLayout, true)
        loadFee()
    }

    private fun loadFee() {
        viewModel.transactionFeeLiveData.observe(this) { response ->
            response?.first?.let {
                showWaiting(binding.includeProgress.progressLayout, false)
                updateViews()
                binding.estimatedTransactionFee.text = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it)
                )
                binding.changeStatusLayout.changeStatusFee.text = getString(
                    R.string.cis_estimated_fee,
                    CurrencyUtil.formatGTU(it)
                )
            }
        }
        viewModel.loadTransactionFee(true)
    }

    private fun updateViews() {
        binding.accountToBakeFrom.text = viewModel.bakerDelegationData.account.address
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
                } else if (viewModel.bakerDelegationData.toSetBakerSuspended == true) {
                    updateViewsSuspendBaker()
                }
            }
        }

        binding.submitBakerTransaction.setOnSliderCompleteListener {
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
        viewModel.transaction.observe(this) { transaction ->
            transaction?.let {
                binding.resultLayout.transactionDivider.visibility = View.VISIBLE
                binding.resultLayout.transactionDetailsButton.visibility = View.VISIBLE
                binding.resultLayout.transactionDetailsButton.setOnClickListener {
                    gotoTransactionDetails(transaction)
                }
            }
        }
    }

    private fun updateViewsRegisterBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_title)
        binding.submitBakerTransaction.setText(
            getString(R.string.baker_registration_confirmation_button)
        )
        binding.bakerRegisterTitle.visibility = View.VISIBLE
        showAmount()
        showRewards()
        showPoolStatus()
        showCommissionRates()
        showMetaUrl()
        showKeys()
        showResultAmount(getString(R.string.baker_register_confirmation_receipt_success))
    }

    private fun updateViewsUpdateBakerKeys() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_keys_title)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_update_affected_account)
        binding.submitBakerTransaction.setText(
            getString(R.string.baker_registration_confirmation_update_keys_button)
        )
        binding.resultLayout.receiptTitle.text =
            getString(R.string.baker_registration_confirmation_update_keys_success)
        showKeys()
        showCommissionRates()
    }

    private fun updateViewsUpdateBakerPool() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_pool_title)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_update_affected_account)
        binding.submitBakerTransaction.setText(
            getString(R.string.baker_registration_confirmation_update_pool_button)
        )
        binding.resultLayout.receiptTitle.text =
            getString(R.string.baker_registration_confirmation_update_pool_success)
        showPoolStatus()
        showCommissionRates()
        showMetaUrl()
    }

    private fun updateViewsUpdateBakerStake() {
        setActionBarTitle(R.string.baker_registration_confirmation_update_stake_title)
        binding.accountToBakeTitle.text =
            getString(R.string.baker_registration_confirmation_update_stake_update)
        binding.submitBakerTransaction.setText(
            getString(R.string.baker_registration_confirmation_update_stake_button)
        )
        showAmount()
        showRewards()
        showCommissionRates()
        showResultAmount(getString(R.string.baker_registration_confirmation_update_stake_success))
    }

    private fun updateViewsRemoveBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_remove_title)
        binding.apply {
            receiptLayout.visibility = View.GONE
            changeStatusLayout.statusLayout.visibility = View.VISIBLE
            changeStatusLayout.changeStatusTitle.text =
                getString(R.string.baker_registration_confirmation_remove_label)
            submitBakerTransaction.setText(
                getString(R.string.baker_registration_confirmation_remove_button)
            )
            resultLayout.receiptTitle.text =
                getString(R.string.baker_registration_confirmation_remove_success)
        }
    }

    private fun updateViewsResumeBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_resume_title)
        binding.apply {
            receiptLayout.visibility = View.GONE
            changeStatusLayout.statusLayout.visibility = View.VISIBLE
            changeStatusLayout.changeStatusDescription.visibility = View.VISIBLE
            changeStatusLayout.changeStatusTitle.text =
                getString(R.string.baker_registration_confirmation_resume_label)
            submitBakerTransaction.setText(
                getString(R.string.baker_registration_confirmation_resume_button)
            )
            resultLayout.receiptTitle.text =
                getString(R.string.baker_registration_confirmation_resume_success)
        }
    }

    private fun updateViewsSuspendBaker() {
        setActionBarTitle(R.string.baker_registration_confirmation_suspend_title)
        binding.apply {
            receiptLayout.visibility = View.GONE
            changeStatusLayout.statusLayout.visibility = View.VISIBLE
            changeStatusLayout.changeStatusDescription.visibility = View.VISIBLE
            changeStatusLayout.changeStatusTitle.text =
                getString(R.string.baker_registration_confirmation_suspend_label)
            submitBakerTransaction.setText(
                getString(R.string.baker_registration_confirmation_suspend_button)
            )
            resultLayout.receiptTitle.text =
                getString(R.string.baker_registration_confirmation_suspend_success)
        }
    }

    private fun showAmount() {
        if (viewModel.stakedAmountHasChanged()) {
            binding.apply {
                delegationAmountConfirmationTitle.visibility = View.VISIBLE
                bakerAmountConfirmation.visibility = View.VISIBLE
                bakerAmountConfirmationDivider.visibility = View.VISIBLE
                bakerAmountConfirmation.text = CurrencyUtil.formatGTU(
                    viewModel.bakerDelegationData.amount ?: BigInteger.ZERO
                )
            }
        }
    }

    private fun showResultAmount(amountDescription: String) {
        binding.apply {
            resultLayout.amountLayout.visibility = View.VISIBLE
            resultLayout.receiptTitle.visibility = View.GONE
            resultLayout.amountDescription.text = amountDescription
            resultLayout.amount.text = CurrencyUtil.formatGTU(
                viewModel.bakerDelegationData.amount ?: BigInteger.ZERO
            )
        }
    }

    private fun showRewards() {
        if (viewModel.restakeHasChanged()) {
            binding.apply {
                rewardsWillBeTitle.visibility = View.VISIBLE
                rewardsWillBe.visibility = View.VISIBLE
                rewardsWillBeDivider.visibility = View.VISIBLE
                rewardsWillBe.text =
                    if (viewModel.bakerDelegationData.restake)
                        getString(R.string.baker_register_confirmation_receipt_added_to_delegation_amount)
                    else
                        getString(R.string.baker_register_confirmation_receipt_at_disposal)
            }
        }
    }

    private fun showPoolStatus() {
        if (viewModel.openStatusHasChanged()) {
            binding.apply {
                poolStatusTitle.visibility = View.VISIBLE
                poolStatus.visibility = View.VISIBLE
                poolStatusDivider.visibility = View.VISIBLE
                poolStatus.text =
                    if (viewModel.isOpenBaker())
                        getString(R.string.baker_register_confirmation_receipt_pool_status_open)
                    else
                        getString(R.string.baker_register_confirmation_receipt_pool_status_closed)
            }
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
                transactionFeeDivider.visibility = View.VISIBLE
                transactionFeeStatus.text =
                    percentFormat.format(viewModel.bakerDelegationData.transactionCommissionRate)
            } else {
                transactionFeeTitle.visibility = View.GONE
                transactionFeeStatus.visibility = View.GONE
                transactionFeeDivider.visibility = View.GONE
            }

            if (viewModel.bakerDelegationData.bakingCommissionRate != null) {
                bakingTitle.visibility = View.VISIBLE
                bakingStatus.visibility = View.VISIBLE
                bakingDivider.visibility = View.VISIBLE
                bakingStatus.text =
                    percentFormat.format(viewModel.bakerDelegationData.bakingCommissionRate)
            } else {
                bakingTitle.visibility = View.GONE
                bakingStatus.visibility = View.GONE
                bakingDivider.visibility = View.GONE
            }
        }
    }

    private fun hideCommissionRates() {
        binding.apply {
            transactionFeeTitle.visibility = View.GONE
            transactionFeeStatus.visibility = View.GONE
            transactionFeeDivider.visibility = View.GONE
            bakingTitle.visibility = View.GONE
            bakingStatus.visibility = View.GONE
            bakingDivider.visibility = View.GONE
        }
    }

    private fun showKeys() {
        binding.apply {
            electionVerifyKeyTitle.visibility = View.VISIBLE
            electionVerifyKey.visibility = View.VISIBLE
            electionVerifyKeyDivider.visibility = View.VISIBLE
            signatureVerifyKeyTitle.visibility = View.VISIBLE
            signatureVerifyKey.visibility = View.VISIBLE
            signatureVerifyKeyDivider.visibility = View.VISIBLE
            aggregationVerifyKeyTitle.visibility = View.VISIBLE
            aggregationVerifyKey.visibility = View.VISIBLE
            aggregationVerifyKeyDivider.visibility = View.VISIBLE
            electionVerifyKey.text = viewModel.bakerDelegationData.bakerKeys?.electionVerifyKey
            signatureVerifyKey.text =
                viewModel.bakerDelegationData.bakerKeys?.signatureVerifyKey
            aggregationVerifyKey.text =
                viewModel.bakerDelegationData.bakerKeys?.aggregationVerifyKey
        }
    }

    private fun showMetaUrl() {
        if (viewModel.metadataUrlHasChanged()) {
            binding.apply {
                metaDataUrlTitle.visibility = View.VISIBLE
                metaDataUrl.visibility = View.VISIBLE
                metaDataDivider.visibility = View.VISIBLE
            }
            if ((viewModel.bakerDelegationData.metadataUrl?.length ?: 0) > 0)
                binding.metaDataUrl.text = viewModel.bakerDelegationData.metadataUrl
            else
                binding.metaDataUrl.text =
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
        if (viewModel.bakerDelegationData.account.balanceAtDisposal <
            (viewModel.bakerDelegationData.cost ?: BigInteger.ZERO)
        ) {
            showNotEnoughFundsForFee()
            return
        }
        viewModel.prepareTransaction()
    }

    private fun showPageAsReceipt() {
        receiptMode = true
        hideActionBarBack(isVisible = false)
        binding.apply {
            submitBakerTransaction.visibility = View.GONE
            submitBakerFinish.visibility = View.VISIBLE
            receiptLayout.visibility = View.GONE
            bakerRegisterTitle.visibility = View.GONE
            changeStatusLayout.statusLayout.visibility = View.GONE
            resultLayout.resultLayout.visibility = View.VISIBLE
            resultLayout.transactionAnimation.playAnimation()
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

    private fun gotoTransactionDetails(transaction: Transaction) {
        val intent = Intent(this, TransactionDetailsActivity::class.java)
        intent.putExtra(
            TransactionDetailsActivity.EXTRA_ACCOUNT,
            viewModel.bakerDelegationData.account
        )
        intent.putExtra(TransactionDetailsActivity.EXTRA_TRANSACTION, transaction)
        intent.putExtra(
            TransactionDetailsActivity.EXTRA_RECEIPT_TITLE,
            getString(R.string.account_baking_pending)
        )
        startActivity(intent)
    }

    private companion object {
        private const val PERCENT_FORMAT_DECIMALS = 3
    }
}
