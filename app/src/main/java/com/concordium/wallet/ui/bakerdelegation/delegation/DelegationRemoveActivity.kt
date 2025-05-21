package com.concordium.wallet.ui.bakerdelegation.delegation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.databinding.ActivityDelegationRemoveBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.dialog.delegation.DelegationNoticeDialog
import com.concordium.wallet.ui.transaction.transactiondetails.TransactionDetailsActivity
import com.concordium.wallet.uicore.button.SliderButton
import com.concordium.wallet.util.UnitConvertUtil
import java.math.BigInteger

class DelegationRemoveActivity : BaseDelegationBakerActivity(
    R.layout.activity_delegation_remove, R.string.delegation_remove_delegation_title
) {
    private lateinit var binding: ActivityDelegationRemoveBinding
    private lateinit var sliderButton: SliderButton
    private var receiptMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegationRemoveBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
    }

    override fun onBackPressed() {
        if (!receiptMode)
            super.onBackPressed()
    }

    override fun initViews() {
        sliderButton = binding.submitDelegationTransaction
        binding.submitDelegationTransaction.setText(getString(R.string.delegation_remove_button))
        binding.estimatedTransactionFee.text = ""

        binding.submitDelegationTransaction.setOnSliderCompleteListener {
            onContinueClicked()
        }

        binding.submitDelegationFinish.setOnClickListener {
            showNotice()
        }

        initializeWaitingLiveData(binding.includeProgress.progressLayout)
        initializeTransactionFeeLiveData(
            binding.includeProgress.progressLayout,
            binding.estimatedTransactionFee
        )
        initializeShowAuthenticationLiveData()

        viewModel.transactionSuccessLiveData.observe(this) { waiting ->
            waiting?.let {
                showPageAsReceipt()
            }
        }
        viewModel.transaction.observe(this) { transaction ->
            transaction?.let {
                binding.apply {
                    includeResultLayout.transactionDivider.visibility = View.VISIBLE
                    includeResultLayout.transactionDetailsButton.visibility = View.VISIBLE
                    includeResultLayout.transactionDetailsButton.setOnClickListener {
                        gotoTransactionDetails(transaction)
                    }
                }
            }
        }

        viewModel.loadTransactionFee(true)

        viewModel.loadChainParameters()
    }

    private fun onContinueClicked() {
        validate()
    }

    private fun validate() {
        if (viewModel.bakerDelegationData.account.balanceAtDisposal <
            (viewModel.bakerDelegationData.cost ?: BigInteger.ZERO)
        ) {
            showNotEnoughFundsForFee()
        } else {
            if (viewModel.bakerDelegationData.isBakerPool) {
                viewModel.bakerDelegationData.account.delegation?.delegationTarget?.bakerId?.let {
                    viewModel.setPoolID(it.toString())
                }
            }
            viewModel.bakerDelegationData.amount = BigInteger.ZERO
            viewModel.prepareTransaction()
        }
    }

    override fun errorLiveData(value: Int) {
        Toast.makeText(this, getString(value), Toast.LENGTH_SHORT).show()
    }

    private fun showPageAsReceipt() {
        receiptMode = true
        hideActionBarBack(isVisible = false)
        binding.apply {
            submitDelegationTransaction.visibility = View.GONE
            submitDelegationFinish.visibility = View.VISIBLE
            receiptLayout.visibility = View.GONE
            includeResultLayout.resultLayout.visibility = View.VISIBLE
            includeResultLayout.receiptTitle.text =
                getString(R.string.delegation_remove_confirm_success)
            includeResultLayout.transactionAnimation.playAnimation()
        }
    }

    private fun showNotice() {
        val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(
            viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0
        )
        val noticeMessage = resources.getQuantityString(
            R.plurals.delegation_notice_message_remove,
            gracePeriod,
            gracePeriod
        )

        DelegationNoticeDialog.newInstance(
            DelegationNoticeDialog.setBundle(noticeMessage)
        ).showSingle(supportFragmentManager, DelegationNoticeDialog.TAG)
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
