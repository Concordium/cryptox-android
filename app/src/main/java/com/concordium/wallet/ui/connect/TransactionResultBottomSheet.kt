package com.concordium.wallet.ui.connect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil.formatGTU
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.math.BigInteger

interface ITransactionResult {
    fun onResultBottomSheetDismissed()
}

class TransactionResultBottomSheet(context: Context, callback: ITransactionResult) : BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme) {

    private val confirmButton: Button?
    private val amountTv: TextView?
    private val taxTv: TextView?
    private val totalTv: TextView?
    private val transactionTv: TextView?
    private val transactionHashTv: TextView?
    private val transactionStatusTv: TextView?
    private val copyImageview: ImageView?

    init {
        setContentView(R.layout.transaction_result_bottomsheet)
        val v = findViewById<View>(R.id.design_bottom_sheet)
        v?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true

        copyImageview = findViewById(R.id.copy_imageview)
        transactionHashTv = findViewById(R.id.transaction_hash_value)
        transactionStatusTv = findViewById(R.id.status_value)
        transactionTv = findViewById(R.id.transaction_value)
        amountTv = findViewById(R.id.amount_value)
        taxTv = findViewById(R.id.net_commission_value)
        totalTv = findViewById(R.id.total_amount_value)
        confirmButton = findViewById(R.id.confirmButton)
        confirmButton?.setOnClickListener {
            callback.onResultBottomSheetDismissed()
        }

        setOnDismissListener {
            callback.onResultBottomSheetDismissed()
        }
    }

    fun setData(transfer: TransactionResult) {
        val am = transfer.amount
        val fee = formatGTU(transfer.fee)
        val total = formatGTU(am + transfer.fee)
        amountTv?.text = context.getString(R.string.amount, formatGTU(am))
        taxTv?.text = context.getString(R.string.amount, fee)
        totalTv?.text = context.getString(R.string.amount, total)
        transactionTv?.text = transfer.title
        transactionHashTv?.text = transfer.submissionId
        transactionStatusTv?.text = transfer.transactionStatus.capitalize()

        copyImageview?.setOnClickListener {
            onCopyClicked(transfer.submissionId)
        }

        show()
    }

    private fun onCopyClicked(value: String) {
        val clipboard: ClipboardManager = context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("transfer.submissionId", value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.submission_id_value_copied), Toast.LENGTH_SHORT).show()
    }
}

data class TransactionResult(
    val amount: BigInteger,
    val fee: BigInteger,
    val title: String,
    val submissionId: String,
    val transactionStatus: String
)