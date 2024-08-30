package com.concordium.wallet.ui.bakerdelegation.delegation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityDelegationRemoveBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.util.UnitConvertUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.math.BigInteger

class DelegationRemoveActivity : BaseDelegationBakerActivity(
    R.layout.activity_delegation_remove, R.string.delegation_remove_delegation_title
) {
    private lateinit var binding: ActivityDelegationRemoveBinding
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
        binding.accountToRemoveDelegateFrom.text =
            (viewModel.bakerDelegationData.account?.name ?: "").plus("\n\n")
                .plus(viewModel.bakerDelegationData.account?.address ?: "")
        binding.estimatedTransactionFee.text = ""

        binding.submitDelegationTransaction.setOnClickListener {
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

        viewModel.transactionSuccessLiveData.observe(this, Observer<Boolean> { waiting ->
            waiting?.let {
                showPageAsReceipt()
            }
        })

        viewModel.loadTransactionFee(true)

        viewModel.loadChainParameters()
    }

    private fun onContinueClicked() {
        validate()
    }

    private fun validate() {
        if (viewModel.atDisposal() < (viewModel.bakerDelegationData.cost ?: BigInteger.ZERO)) {
            showNotEnoughFunds()
        } else {
            if (viewModel.bakerDelegationData.isBakerPool) {
                viewModel.bakerDelegationData.account?.delegation?.delegationTarget?.bakerId?.let {
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
        binding.delegationRemoveText.visibility = View.GONE
        binding.submitDelegationTransaction.visibility = View.GONE
        binding.submitDelegationFinish.visibility = View.VISIBLE
        binding.includeTransactionSubmittedHeader.transactionSubmitted.visibility = View.VISIBLE
        viewModel.bakerDelegationData.submissionId?.let { submissionId ->
            binding.transactionHashView.isVisible = true
            binding.transactionHashView.transactionHash = submissionId
        }
    }

    private fun showNotice() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.delegation_notice_title)
        val gracePeriod = UnitConvertUtil.secondsToDaysRoundedDown(
            viewModel.bakerDelegationData.chainParameters?.delegatorCooldown ?: 0
        )
        builder.setMessage(
            resources.getQuantityString(
                R.plurals.delegation_notice_message_remove,
                gracePeriod,
                gracePeriod
            )
        )
        builder.setPositiveButton(getString(R.string.delegation_notice_ok)) { dialog, _ ->
            dialog.dismiss()
            finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
        }
        builder.create().show()
    }

    override fun showWaiting(progressLayout: View, waiting: Boolean) {
        super.showWaiting(progressLayout, waiting)
        binding.submitDelegationTransaction.isEnabled = !waiting
    }
}
