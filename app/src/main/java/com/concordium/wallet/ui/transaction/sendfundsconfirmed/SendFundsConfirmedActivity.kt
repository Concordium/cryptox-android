package com.concordium.wallet.ui.transaction.sendfundsconfirmed

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivitySendFundsConfirmedBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.CBORUtil

class SendFundsConfirmedActivity :
    BaseActivity(R.layout.activity_send_funds_confirmed, R.string.send_funds_confirmed_title) {

    companion object {
        const val EXTRA_TRANSFER = "EXTRA_TRANSFER"
        const val EXTRA_RECIPIENT = "EXTRA_RECIPIENT"
    }

    private lateinit var viewModel: SendFundsConfirmedViewModel
    private val binding by lazy {
        ActivitySendFundsConfirmedBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transfer = intent.extras!!.getSerializable(EXTRA_TRANSFER) as Transfer
        val recipient = intent.extras!!.getSerializable(EXTRA_RECIPIENT) as Recipient

        initializeViewModel()
        viewModel.initialize(transfer, recipient)
        initViews()

        closeBtn?.visibility = View.VISIBLE
        closeBtn?.setOnClickListener {
            gotoAccountDetails()
        }
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    // endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SendFundsConfirmedViewModel::class.java]
    }

    private fun initViews() {
        hideActionBarBack(false)

        binding.amountTextview.text = CurrencyUtil.formatGTU(viewModel.transfer.amount, withGStroke = true)
        binding.feeTextview.text = getString(
                R.string.send_funds_confirmed_fee_info,
                CurrencyUtil.formatGTU(viewModel.transfer.cost)
            )

        if (viewModel.transfer.memo.isNullOrEmpty()) {
            binding.memoConfirmationTextview.visibility = View.GONE
        } else {
            binding.memoConfirmationTextview.visibility = View.VISIBLE
            binding.memoConfirmationTextview.text = getString(
                R.string.send_funds_confirmation_memo,
                viewModel.transfer.memo?.let { CBORUtil.decodeHexAndCBOR(it) } ?: ""
            )
        }

        binding.confirmButton.setOnClickListener {
            gotoAccountDetails()
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun gotoAccountDetails() {
        val intent = Intent(this, AccountDetailsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    //endregion
}
