package com.concordium.wallet.ui.common

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.util.DateTimeUtil
import java.math.BigInteger

object TransactionViewHelper {
    @SuppressLint("SetTextI18n")
    fun show(
        ta: Transaction,
        titleTextView: TextView,
        subHeaderTextView: TextView,
        totalTextView: TextView,
        costTextView: TextView,
        memoLayout: LinearLayout,
        memoTextView: TextView,
        alertImageView: ImageView,
        statusImageView: ImageView,
        showDate: Boolean = false,
        isReceipt: Boolean = false
    ) {
        // Title
        titleTextView.text =
            if (isReceipt)
                titleTextView.context.getString(R.string.transaction_type_transfer)
            else
                ta.title

        memoTextView.text = ta.getDecryptedMemo()
        memoLayout.visibility = if (ta.hasMemo()) View.VISIBLE else View.GONE

        // Time
        subHeaderTextView.text = if (showDate) {
            DateTimeUtil.formatDateAsLocalMediumWithTime(ta.timeStamp)
        } else {
            DateTimeUtil.formatTimeAsLocal(ta.timeStamp)
        }

        fun setTotalView(total: BigInteger) {
            totalTextView.text = CurrencyUtil.formatGTU(total)
            val textColor = if (total.signum() > 0)
                ContextCompat.getColor(
                    totalTextView.context,
                    R.color.mw24_green
                ) else
                ContextCompat.getColor(
                    totalTextView.context,
                    R.color.cryptox_white_main
                )
            totalTextView.setTextColor(textColor)
            totalTextView.visibility = View.VISIBLE
        }

        fun showTransactionFeeText() {
            costTextView.visibility = View.VISIBLE
            costTextView.text =
                costTextView.context.getString(R.string.account_details_shielded_transaction_fee)
        }

        fun hideCostLine() {
            costTextView.visibility = View.GONE
        }

        fun showCostLineWithAmounts() {
            // Subtotal and cost
            if (ta.subtotal != null && ta.cost != null) {
                costTextView.visibility = View.VISIBLE

                val cost = ta.cost
                var costPrefix = ""

                if (ta.transactionStatus == TransactionStatus.RECEIVED ||
                    (ta.transactionStatus == TransactionStatus.COMMITTED && ta.outcome == TransactionOutcome.Ambiguous)
                    || ta.transactionStatus == TransactionStatus.ABSENT
                ) {
                    costPrefix = "~"
                }

                costTextView.text = costTextView.context.getString(R.string.account_details_fee) +
                        " $costPrefix${
                            CurrencyUtil.formatGTU(cost)
                        } " + costTextView.context.getString(R.string.accounts_overview_balance_suffix)
            } else {
                costTextView.visibility = View.GONE
            }
        }

        //Clear first
        totalTextView.text = ""

        // Public balance
        // remote transactions
        if (ta.isRemoteTransaction()) {
            // simpleTransfer (as before: so use total subtotal and cost for display)
            // transferToSecret (as simpleTransfer)
            // transferToPublic (as simpleTransfer)
            // update (send tokens)
            if (ta.isSimpleTransfer() || ta.isTransferToSecret() || ta.isTransferToPublic() || ta.isSmartContractUpdate()) {
                setTotalView(ta.getTotalAmountForRegular())
                showCostLineWithAmounts()
            } else
            // encryptedTransfer
            //    if origin is self => show as simpleTransfer, but show subtotal/cost row as "Shielded transaction fee
            //    else => do NOT show
                if (ta.isEncryptedTransfer()) {
                    setTotalView(ta.getTotalAmountForRegular())
                    if (ta.isOriginSelf()) {
                        showTransactionFeeText()
                    } else {
                        // left empty intentionally (won't be called as item is filtered out)
                    }
                } else { // baker
                    setTotalView(ta.getTotalAmountForRegular())
                    showCostLineWithAmounts()
                }
        }
        // local (unfinalized) transactions
        else {

            // simpleTransfer (handle as before)
            // transferToSecret (as simpleTransfer)
            if (ta.isSimpleTransfer() || ta.isTransferToSecret()) {
                setTotalView(ta.getTotalAmountForRegular())
                showCostLineWithAmounts()
            } else
            // update Smart Contract (show the fee as an estimate cost until the transaction is Finalized)
                if (ta.isSmartContractUpdate()) {
                    setTotalView(ta.getTotalAmountForSmartContractUpdate())
                    showCostLineWithAmounts()
                } else
                // transferToPublic (show only the cost as total, no subtotal or fee on second row - clarified with Concordium)
                    if (ta.isTransferToPublic()) {
                        setTotalView(ta.getTotalAmountForRegular())
                        hideCostLine()
                    } else
                    // encryptedTransfer (show only the cost as total, subtotal/cost row should be "Shielded transaction fee")
                        if (ta.isEncryptedTransfer()) {
                            setTotalView(ta.getTotalAmountForRegular())
                            showTransactionFeeText()
                        }
        }

        // Alert image
        if (ta.transactionStatus == TransactionStatus.ABSENT ||
            (ta.transactionStatus == TransactionStatus.COMMITTED && ta.outcome == TransactionOutcome.Reject) ||
            (ta.transactionStatus == TransactionStatus.FINALIZED && ta.outcome == TransactionOutcome.Reject)
        ) {
            alertImageView.visibility = View.VISIBLE
        } else {
            alertImageView.visibility = View.GONE
        }

        // Status image
        if (ta.transactionStatus == TransactionStatus.RECEIVED ||
            (ta.transactionStatus == TransactionStatus.COMMITTED && ta.outcome == TransactionOutcome.Ambiguous)
        ) {
            statusImageView.setImageDrawable(
                ContextCompat.getDrawable(statusImageView.context, R.drawable.ic_time)
            )
        } else if (ta.transactionStatus == TransactionStatus.COMMITTED) {
            statusImageView.setImageDrawable(
                ContextCompat.getDrawable(statusImageView.context, R.drawable.ic_ok)
            )
        } else if (ta.transactionStatus == TransactionStatus.FINALIZED) {
            statusImageView.setImageDrawable(
                ContextCompat.getDrawable(statusImageView.context, R.drawable.ic_ok_x2)
            )
        } else {
            statusImageView.setImageDrawable(null)
        }
    }
}
