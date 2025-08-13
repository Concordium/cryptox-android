package com.concordium.wallet.ui.transaction.transactiondetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOriginType
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityTransactionDetailsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.TransactionViewHelper
import com.concordium.wallet.uicore.toast.showGradientToast
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.getSerializable

class TransactionDetailsActivity : BaseActivity(
    R.layout.activity_transaction_details,
    R.string.transaction_details_title
) {

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
        const val EXTRA_TRANSACTION = "EXTRA_TRANSACTION"
        const val EXTRA_RECEIPT_TITLE = "EXTRA_IS_RECEIPT"
    }

    private lateinit var viewModel: TransactionDetailsViewModel
    private val binding by lazy {
        ActivityTransactionDetailsBinding.bind(findViewById(R.id.root_layout))
    }
    private var receiptTitle = "" //always show "Transfer" title if show send receipt

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)
        val transaction = intent.getSerializable(EXTRA_TRANSACTION, Transaction::class.java)
        receiptTitle = intent.getStringExtra(EXTRA_RECEIPT_TITLE) ?: ""

        initializeViewModel()
        viewModel.initialize(account, transaction)
        initViews()
        viewModel.showData()

        hideActionBarBack(isVisible = true)
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TransactionDetailsViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showErrorMessage(value)
            }
        })
        viewModel.showDetailsLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                showTransactionDetails(receiptTitle)
            }
        })
    }

    private fun initViews() {
        binding.contentLayout.visibility = View.GONE
        binding.messageTextview.visibility = View.GONE
        binding.fromAddressLayout.visibility = View.GONE
        binding.toAddressLayout.visibility = View.GONE
        binding.transactionHashLayout.visibility = View.GONE
        binding.blockHashLayout.visibility = View.GONE
        binding.detailsLayout.visibility = View.GONE
        binding.memoLayout.visibility = View.GONE
        binding.transactionDateLayout.visibility = View.GONE

        binding.viewOnExplorerButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.getExplorerUrl()))
            ContextCompat.startActivity(this, browserIntent, null)
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun onCopyClicked(title: String, value: String) {
        val clipboard: ClipboardManager =
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(title, value)
        clipboard.setPrimaryClip(clip)

        showGradientToast(
            iconResId = R.drawable.mw24_ic_address_copy_check,
            title = getString(R.string.transaction_details_value_copied, title.trim(':'))
        )
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun showErrorMessage(@StringRes stringRes: Int) {
        Toast.makeText(applicationContext, stringRes, Toast.LENGTH_SHORT).show()
    }

    private fun showTransactionDetails(receiptTitle: String) {
        binding.contentLayout.visibility = View.VISIBLE
        showDetailsContent(receiptTitle)
    }

    private fun showDetailsContent(receiptTitle: String) {
        val transaction = viewModel.transaction
        val transactionItem = binding.transactionItem
        binding.transactionItem.itemRootLayout.background = null

        TransactionViewHelper.show(
            transaction,
            transactionItem.titleTextview,
            transactionItem.subheaderTextview,
            transactionItem.totalTextview,
            transactionItem.costTextview,
            transactionItem.layoutMemo,
            transactionItem.memoTextview,
            transactionItem.alertImageview,
            transactionItem.statusImageview,
            titleFromReceipt = receiptTitle
        )
        // Do not show the memo in the item,
        // it is shown below with ability to copy the value.
        transactionItem.layoutMemo.isVisible = false
        transactionItem.arrowImageview.isVisible = false

        showDate(transaction)
        showRejectReason(transaction)
        showFromAddressOrOrigin(transaction)
        showToAddress(transaction)
        showTransactionHash(transaction)
        showBlockHashes(transaction)
        showEvents(transaction)
        showMemo(transaction)
        showExplorerButton(transaction)

        enableCopy(transaction)
    }

    private fun enableCopy(ta: Transaction) {
        if (ta.hash != null) {
            binding.copyButton.setOnClickListener {
                onCopyClicked(
                    getString(R.string.transaction_details_transaction_hash),
                    ta.hash!!
                )
            }
        } else {
            ta.blockHashes?.let {
                val blockHashesString = StringBuilder("")
                var isFirst = true
                for (blockHash in it) {
                    if (!isFirst) {
                        blockHashesString.append("\n\n")
                    }
                    blockHashesString.append(blockHash)
                    isFirst = false
                }
                binding.copyButton.setOnClickListener {
                    onCopyClicked(
                        getString(R.string.transaction_details_block_hash),
                        blockHashesString.toString()
                    )
                }
            }
        }
    }

    private fun showDate(ta: Transaction) {
        val time = DateTimeUtil.formatDateAsLocalMediumWithTime(ta.timeStamp)
        binding.transactionDateLayout.visibility = View.VISIBLE
        binding.transactionDateLayout.setValue(time)
        binding.transactionDateLayout.setDivider(false)
    }

    private fun showMemo(ta: Transaction) {
        if (ta.memoText != null) {
            binding.memoLayout.visibility = View.VISIBLE
            binding.memoLayout.setValue(ta.memoText)
        } else {
            binding.memoLayout.visibility = View.GONE
        }
    }

    private fun showRejectReason(ta: Transaction) {
        if (ta.rejectReason != null) {
            binding.memoLayout.visibility = View.VISIBLE
            binding.messageTextview.text = ta.rejectReason
        } else {
            binding.memoLayout.visibility = View.GONE
        }
    }

    private fun showFromAddressOrOrigin(ta: Transaction) {
        binding.fromAddressLayout.visibility = View.GONE
        if (ta.fromAddress != null) {
            binding.fromAddressLayout.visibility = View.VISIBLE
            binding.fromAddressLayout.setTitle(getString(R.string.transaction_details_from_address))
            binding.fromAddressLayout.setValue(ta.fromAddress)
        } else {
            val origin = ta.origin
            val type = origin?.type
            val address = origin?.address
            if (origin != null && address != null) {
                if (type == TransactionOriginType.Account) {
                    binding.fromAddressLayout.visibility = View.VISIBLE
                    binding.fromAddressLayout.setTitle(getString(R.string.transaction_details_origin))
                    binding.fromAddressLayout.setValue(origin.address, true)
                }
            }
        }
    }

    private fun showToAddress(ta: Transaction) {
        if (ta.toAddress != null) {
            binding.toAddressLayout.visibility = View.VISIBLE
            binding.toAddressLayout.setTitle(getString(R.string.transaction_details_to_address))
            binding.toAddressLayout.setValue(ta.toAddress)
        } else {
            binding.toAddressLayout.visibility = View.GONE
        }
    }

    private fun showTransactionHash(ta: Transaction) {
        val transactionHash = ta.hash
        if (transactionHash != null) {
            binding.transactionHashLayout.visibility = View.VISIBLE
            binding.transactionHashLayout.setValue(transactionHash, true)
        } else {
            binding.transactionHashLayout.visibility = View.GONE
        }
    }

    private fun showBlockHashes(ta: Transaction) {
        if (ta.status == TransactionStatus.RECEIVED) {
            binding.blockHashLayout.visibility = View.VISIBLE
            binding.blockHashLayout.setValue(
                getString(R.string.transaction_details_block_hash_submitted),
                true
            )
        } else if (ta.status == TransactionStatus.ABSENT) {
            binding.blockHashLayout.visibility = View.VISIBLE
            binding.blockHashLayout.setValue(
                getString(R.string.transaction_details_block_hash_failed),
                true
            )
        } else {
            val blockHashes = ta.blockHashes
            if (!blockHashes.isNullOrEmpty()) {
                binding.blockHashLayout.visibility = View.VISIBLE
                val blockHashesString = StringBuilder("")
                var isFirst = true
                for (blockHash in blockHashes) {
                    if (!isFirst) {
                        blockHashesString.append("\n\n")
                    }
                    blockHashesString.append(blockHash)
                    isFirst = false
                }
                binding.blockHashLayout.setValue(blockHashesString.toString(), true)
            } else {
                binding.blockHashLayout.visibility = View.GONE
            }
        }
    }

    private fun showEvents(ta: Transaction) {
        val events = ta.events
        if (!events.isNullOrEmpty() &&
            ta.outcome == TransactionOutcome.Success &&
            ta.fromAddress == null && ta.toAddress == null
        ) {
            binding.detailsLayout.visibility = View.VISIBLE
            val eventsString = StringBuilder("")
            var isFirst = true
            for (event in events) {
                if (!isFirst) {
                    eventsString.append("\n\n")
                }
                eventsString.append(event)
                isFirst = false
            }
            binding.detailsLayout.setValue(eventsString.toString())
        } else {
            binding.detailsLayout.visibility = View.GONE
        }
    }

    private fun showExplorerButton(ta: Transaction) {
        binding.viewOnExplorerButton.isVisible = ta.hash != null
    }

    //endregion
}
