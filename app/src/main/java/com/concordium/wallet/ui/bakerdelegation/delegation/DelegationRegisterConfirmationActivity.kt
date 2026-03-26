package com.concordium.wallet.ui.bakerdelegation.delegation

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_DELEGATION
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_DELEGATION
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityDelegationRegistrationConfirmationBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.dialog.delegation.DelegationErrorDialog
import com.concordium.wallet.ui.bakerdelegation.dialog.delegation.DelegationNoticeDialog
import com.concordium.wallet.ui.transaction.transactiondetails.TransactionDetailsActivity
import com.concordium.wallet.uicore.button.SliderButton
import com.concordium.wallet.util.UnitConvertUtil
import java.math.BigInteger

class DelegationRegisterConfirmationActivity : BaseDelegationBakerActivity(
    R.layout.activity_delegation_registration_confirmation,
    R.string.delegation_register_delegation_title
) {
    private var receiptMode = false
    private lateinit var binding: ActivityDelegationRegistrationConfirmationBinding
    private lateinit var sliderButton: SliderButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            ActivityDelegationRegistrationConfirmationBinding.bind(findViewById(R.id.root_layout))
        setContentView(binding.root)
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
        sliderButton = binding.submitDelegationTransaction
        viewModel.chainParametersLoadedLiveData.observe(this) { success ->
            success?.let {
                updateViews()
                showWaiting(binding.includeProgress.progressLayout, false)
            }
        }
        showWaiting(binding.includeProgress.progressLayout, true)
        viewModel.loadChainParameters()
        binding.legalDisclaimerButton.setOnClickListener {
            showLegalDisclaimerDialog()
        }
        binding.networkTextView.text = App.appCore.session.network.name
    }

    private fun updateViews() {
        if (viewModel.isUpdatingDelegation()) {
            setActionBarTitle(R.string.delegation_update_delegation_title)
            binding.includeResultLayout.amountDescription.text =
                getString(R.string.delegation_update_delegation_success)
            binding.submitDelegationTransaction.setText(
                getString(R.string.delegation_update_delegation_button)
            )
        }

        if (viewModel.bakerDelegationData.type == REGISTER_DELEGATION) {
            binding.includeResultLayout.amountDescription.text =
                getString(R.string.delegation_status_content_registered_success)
            binding.submitDelegationTransaction.setText(
                getString(R.string.delegation_status_content_registered_button)
            )
        }

        binding.submitDelegationTransaction.setOnSliderCompleteListener {
            onContinueClicked()
        }

        binding.submitDelegationFinish.setOnClickListener {
            showNotice()
        }

        binding.accountToDelegateFrom.text = getAccountName()
        binding.delegationAmountConfirmation.text =
            CurrencyUtil.formatGTU(viewModel.bakerDelegationData.amount ?: BigInteger.ZERO)
        binding.includeResultLayout.amount.text =
            CurrencyUtil.formatGTU(viewModel.bakerDelegationData.amount ?: BigInteger.ZERO)
        binding.targetPool.text =
            if (viewModel.bakerDelegationData.isLPool)
                getString(R.string.delegation_register_delegation_passive_long)
            else
                getString(R.string.delegation_register_delegation_pool_targeted)
        binding.rewardsWillBe.text =
            if (viewModel.bakerDelegationData.restake) getString(R.string.delegation_status_added_to_delegation_amount) else getString(
                R.string.delegation_status_at_disposal
            )

        if (!viewModel.poolHasChanged() && viewModel.isUpdatingDelegation()) {
            binding.fromToIcon.visibility = View.GONE
            binding.targetPool.visibility = View.GONE
        }
        if (!viewModel.restakeHasChanged()) {
            binding.rewardsWillBeTitle.visibility = View.GONE
            binding.rewardsWillBe.visibility = View.GONE
            binding.rewardsDivider.visibility = View.GONE
        }

        initializeTransactionFeeLiveData(
            binding.includeProgress.progressLayout,
            binding.estimatedTransactionFee
        )
        initializeShowAuthenticationLiveData()
        initializeWaitingLiveData(binding.includeProgress.progressLayout)

        viewModel.transactionSuccessLiveData.observe(this) { waiting ->
            waiting?.let {
                showPageAsReceipt()
            }
        }
        viewModel.transaction.observe(this) { transaction ->
            transaction?.let {
                binding.includeResultLayout.apply {
                    transactionDivider.visibility = View.VISIBLE
                    transactionDetailsButton.visibility = View.VISIBLE
                    transactionDetailsButton.setOnClickListener {
                        gotoTransactionDetails(transaction)
                    }
                }
            }
        }

        viewModel.loadTransactionFee(true)
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            DelegationErrorDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            if (DelegationErrorDialog.getResult(bundle)) {
                onContinueClicked()
            }
        }
    }

    override fun errorLiveData(value: Int) {
        val messageFromWalletProxy = getString(value)

        DelegationErrorDialog.newInstance(
            DelegationErrorDialog.setBundle(
                getString(
                    R.string.delegation_register_delegation_failed_message,
                    messageFromWalletProxy
                )
            )
        ).showSingle(supportFragmentManager, DelegationErrorDialog.TAG)
    }

    private fun getAccountName(): String {
        return if (viewModel.bakerDelegationData.account.getAccountName() ==
            Account.getDefaultName(viewModel.bakerDelegationData.account.address)
        )
            viewModel.bakerDelegationData.account.getAccountName()
        else
            viewModel.bakerDelegationData.account.getAccountName()
                .plus("\n\n")
                .plus(Account.getDefaultName(viewModel.bakerDelegationData.account.address))
    }

    private fun showPageAsReceipt() {
        receiptMode = true
        hideActionBarBack(isVisible = false)
        binding.apply {
            submitDelegationTransaction.visibility = View.GONE
            submitDelegationFinish.visibility = View.VISIBLE
            receiptLayout.visibility = View.GONE
            includeResultLayout.resultLayout.visibility = View.VISIBLE
            includeResultLayout.receiptTitle.visibility = View.GONE
            includeResultLayout.amountLayout.visibility = View.VISIBLE
            includeResultLayout.transactionAnimation.playAnimation()
        }
    }

    private fun onContinueClicked() {
        viewModel.prepareTransaction()
    }

    private fun showNotice() {
        val noticeMessage: String

        if (viewModel.isInCoolDown()) {
            noticeMessage = getString(R.string.delegation_notice_message_locked)
        } else {
            var gracePeriod = 0
            viewModel.bakerDelegationData.chainParameters?.delegatorCooldown?.let { delegatorCooldown ->
                gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(delegatorCooldown)
            }
            when (viewModel.bakerDelegationData.type) {
                UPDATE_DELEGATION -> {
                    noticeMessage = if ((viewModel.bakerDelegationData.amount
                            ?: BigInteger.ZERO) < (viewModel.bakerDelegationData.oldStakedAmount
                            ?: BigInteger.ZERO)
                    ) {
                        resources.getQuantityString(
                            R.plurals.delegation_notice_message_decrease,
                            gracePeriod,
                            gracePeriod
                        )
                    } else {
                        getString(R.string.delegation_notice_message)
                    }
                }

                REMOVE_DELEGATION -> {
                    noticeMessage = resources.getQuantityString(
                        R.plurals.delegation_notice_message_remove,
                        gracePeriod,
                        gracePeriod
                    )
                }

                else -> {
                    noticeMessage = getString(R.string.delegation_notice_message)
                }
            }
        }

        DelegationNoticeDialog.newInstance(
            DelegationNoticeDialog.setBundle(
                noticeMessage,
                viewModel.isHasShowReviewDialogAfterEarnSetup.not()
            )
        ).showSingle(supportFragmentManager, DelegationNoticeDialog.TAG)
        viewModel.setHasShowReviewDialogAfterEarnSetup()
    }

    override fun showWaiting(progressLayout: View, waiting: Boolean) {
        super.showWaiting(progressLayout, waiting)
        binding.submitDelegationTransaction.isEnabled = !waiting
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
            getString(R.string.account_delegation_pending)
        )
        startActivity(intent)
    }
}
