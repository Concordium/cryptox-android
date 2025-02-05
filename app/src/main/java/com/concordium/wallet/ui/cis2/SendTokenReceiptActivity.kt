package com.concordium.wallet.ui.cis2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.airbnb.lottie.LottieDrawable
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendTokenReceiptBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.transaction.transactiondetails.TransactionDetailsActivity
import com.concordium.wallet.uicore.button.SliderButton
import com.concordium.wallet.util.CBORUtil
import com.concordium.wallet.util.getSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SendTokenReceiptActivity : BaseActivity(
    R.layout.activity_send_token_receipt,
    R.string.cis_send_funds_confirmation
), AuthDelegate by AuthDelegateImpl() {
    private lateinit var binding: ActivitySendTokenReceiptBinding
    private lateinit var sliderButton: SliderButton
    private val viewModel: SendTokenViewModel by viewModels()
    private var receiptMode = false

    companion object {
        const val PARENT_ACTIVITY = "PARENT_ACTIVITY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.sendTokenData = intent.getSerializable(SEND_TOKEN_DATA, SendTokenData::class.java)
        binding = ActivitySendTokenReceiptBinding.bind(findViewById(R.id.root_layout))
        initViews()
        initObservers()
    }

    override fun onBackPressed() {
        if (!receiptMode)
            super.onBackPressed()
    }

    private fun initViews() {
        sliderButton = binding.sendFunds
        binding.senderName.text = viewModel.sendTokenData.account?.getAccountName()
        binding.senderAddress.text = viewModel.sendTokenData.account?.address
        binding.amountTitle.text =
            if (viewModel.sendTokenData.token?.isUnique == true)
                getString(R.string.cis_token_quantity)
            else
                getString(
                    R.string.cis_amount,
                    viewModel.sendTokenData.token?.symbol
                )
        binding.amount.text =
            CurrencyUtil.formatGTU(viewModel.sendTokenData.amount, viewModel.sendTokenData.token)
        binding.receiver.text = viewModel.sendTokenData.receiver
        viewModel.sendTokenData.receiverName?.let {
            binding.receiverName.visibility = View.VISIBLE
            binding.receiverName.text = it
        }
        binding.fee.text =
            getString(
                R.string.cis_estimated_fee,
                CurrencyUtil.formatGTU(viewModel.sendTokenData.fee!!, false)
            )
        CoroutineScope(Dispatchers.Default).launch {
            runOnUiThread {
                showPageAsSendPrompt()
            }
        }
        binding.statusAmount.text = CurrencyUtil.formatGTU(viewModel.sendTokenData.amount)
        binding.transactionSymbol.text =
            if (viewModel.sendTokenData.token?.isUnique == true)
                getString(R.string.cis_token_quantity)
            else
                viewModel.sendTokenData.token?.symbol

        binding.sendFunds.setOnSliderCompleteListener {
            onSend()
        }
        binding.finish.setOnClickListener {
            onFinish()
        }
        viewModel.sendTokenData.memo.let { encodedMemo ->
            if (encodedMemo != null) {
                binding.memoDivider.visibility = View.VISIBLE
                binding.memoLayout.visibility = View.VISIBLE
                binding.memo.text = CBORUtil.decodeHexAndCBOR(encodedMemo)
            } else {
                binding.memoDivider.visibility = View.GONE
                binding.memoLayout.visibility = View.GONE
            }
        }
    }

    private fun onSend() {
        viewModel.send()
    }

    private fun onFinish() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun initObservers() {
        viewModel.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        viewModel.transactionWaiting.observe(this) {
            if (it)
                showPageAsReceipt(TransactionProcessingStatus.LOADING)
        }

        viewModel.showAuthentication.observe(this) {
            showAuthentication(
                activity = this@SendTokenReceiptActivity,
                onAuthenticated = viewModel::continueWithPassword,
                onCanceled = {
                    sliderButton.transitionToStart()
                }
            )
        }

        viewModel.transactionReady.observe(this) {
            showPageAsReceipt(TransactionProcessingStatus.SUCCESS)
        }

        viewModel.errorInt.observe(this) {
            showPageAsReceipt(TransactionProcessingStatus.FAIL)
        }
        viewModel.transaction.observe(this) { transaction ->
            if (transaction != null)
                binding.transactionDetails.setOnClickListener {
                    gotoTransactionDetails(transaction)
                }
        }
    }

    private fun showPageAsReceipt(status: TransactionProcessingStatus) {
        receiptMode = true
        hideActionBarBack(isVisible = false)
        binding.sendMainLayout.visibility = View.GONE
        binding.sendStatusLayout.visibility = View.VISIBLE
        binding.sendFunds.visibility = View.GONE
        binding.finish.visibility = View.VISIBLE

        when (status) {
            TransactionProcessingStatus.LOADING -> {
                binding.apply {
                    transactionStatusLabel.text = getString(R.string.cis_transaction_in_progress)
                    transactionAnimation.setAnimation(R.raw.transaction_loading)
                    transactionAnimation.repeatCount = LottieDrawable.INFINITE
                    transactionAnimation.playAnimation()
                    showTransactionDetails(false)
                }
            }

            TransactionProcessingStatus.SUCCESS -> {
                binding.apply {
                    transactionStatusLabel.text = getString(R.string.cis_transaction_success)
                    transactionAnimation.setAnimation(R.raw.transaction_success)
                    transactionAnimation.repeatCount = 0
                    transactionAnimation.playAnimation()
                    showTransactionDetails(true)
                }
            }

            TransactionProcessingStatus.FAIL -> {
                binding.apply {
                    transactionStatusLabel.text = getString(R.string.cis_transaction_fail)
                    transactionAnimation.setAnimation(R.raw.transaction_fail)
                    transactionAnimation.repeatCount = 0
                    transactionAnimation.playAnimation()
                    showTransactionDetails(false)
                }
            }
        }
    }

    private fun showTransactionDetails(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.transactionDivider.visibility = visibility
        binding.transactionDetails.visibility = visibility
    }

    private fun showPageAsSendPrompt() {
        receiptMode = false
        hideActionBarBack(isVisible = true)
        binding.sendStatusLayout.visibility = View.GONE
        binding.sendMainLayout.visibility = View.VISIBLE
        binding.finish.visibility = View.GONE
        binding.sendFunds.visibility = View.VISIBLE
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressLayout.visibility = if (waiting) View.VISIBLE else View.GONE
    }

    private fun gotoTransactionDetails(transaction: Transaction) {
        val intent = Intent(this, TransactionDetailsActivity::class.java)
        intent.putExtra(
            TransactionDetailsActivity.EXTRA_ACCOUNT,
            viewModel.sendTokenData.account
        )
        intent.putExtra(TransactionDetailsActivity.EXTRA_TRANSACTION, transaction)
        intent.putExtra(TransactionDetailsActivity.EXTRA_IS_RECEIPT, true)
        startActivity(intent)
    }
}
