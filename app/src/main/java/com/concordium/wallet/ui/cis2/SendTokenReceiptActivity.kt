package com.concordium.wallet.ui.cis2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendTokenReceiptBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.SendTokenViewModel.Companion.SEND_TOKEN_DATA
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
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
                getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(viewModel.sendTokenData.fee!!, false)
                )
            )
        CoroutineScope(Dispatchers.Default).launch {
            runOnUiThread {
                showPageAsSendPrompt()
            }
        }
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

        viewModel.showAuthentication.observe(this) {
            showAuthentication(
                activity = this@SendTokenReceiptActivity,
                onAuthenticated = viewModel::continueWithPassword,
                onCanceled = {
                    sliderButton.transitionToStart()
                }
            )
        }

        viewModel.transactionReady.observe(this) { submissionId ->
            binding.transactionHashView.isVisible = true
            binding.transactionHashView.transactionHash = submissionId
            showPageAsReceipt()
        }

        viewModel.errorInt.observe(this) {
            Toast.makeText(this, getString(it), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPageAsReceipt() {
        receiptMode = true
        hideActionBarBack(isVisible = false)
        binding.sendFunds.visibility = View.GONE
        binding.finish.visibility = View.VISIBLE
    }

    private fun showPageAsSendPrompt() {
        receiptMode = false
        hideActionBarBack(isVisible = true)
        binding.sendFunds.visibility = View.VISIBLE
        binding.finish.visibility = View.GONE
    }

    private fun showWaiting(waiting: Boolean) {
        binding.includeProgress.progressLayout.visibility = if (waiting) View.VISIBLE else View.GONE
    }
}
