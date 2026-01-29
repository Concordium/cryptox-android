package com.concordium.wallet.ui.common

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
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
        memoLayout: ConstraintLayout,
        memoTextView: TextView,
        showDate: Boolean = false,
        titleFromReceipt: String = "",
        memoExpandButton: ImageView? = null,
        isMemoExpanded: Boolean = false,
    ) {
        // Title
        titleTextView.text = titleFromReceipt.ifEmpty { ta.title }
        titleTextView.setTextColor(
            when {
                ta.isBakerSuspension() ||
                        ta.isBakerPrimingForSuspension() ||
                        ta.status == TransactionStatus.ABSENT ||
                        (ta.status == TransactionStatus.COMMITTED && ta.outcome == TransactionOutcome.Reject) ||
                        (ta.status == TransactionStatus.FINALIZED && ta.outcome == TransactionOutcome.Reject)
                -> ContextCompat.getColor(titleTextView.context, R.color.mw24_attention_red)

                else -> ContextCompat.getColor(titleTextView.context, R.color.cryptox_white_main)
            }
        )

        // Memo
        memoTextView.text = ta.memoText
        memoTextView.maxLines = if (isMemoExpanded) Int.MAX_VALUE else 1
        memoTextView.ellipsize = if (isMemoExpanded) null else TextUtils.TruncateAt.END
        memoExpandButton?.setImageResource(
            if (isMemoExpanded) R.drawable.mw24_ic_transaction_collapse
            else R.drawable.mw24_ic_transaction_expand
        )

        memoLayout.isVisible = ta.memoText != null

        // Time
        subHeaderTextView.text = if (showDate) {
            DateTimeUtil.formatDateAsLocalMediumWithTime(ta.timeStamp)
        } else {
            DateTimeUtil.formatTimeAsLocal(ta.timeStamp)
        }

        // Amount
        val amountValue =
            ta.tokenTransferAmount?.value
                ?.let { tokenAmount ->
                    if (ta.isOriginSelf())
                        -tokenAmount
                    else
                        tokenAmount
                }
                ?: ta.subtotal
                ?: ta.total

        if (amountValue == BigInteger.ZERO) {
            totalTextView.visibility = View.GONE
        } else {
            totalTextView.visibility = View.VISIBLE

            val amountDecimals =
                ta.tokenTransferAmount?.decimals
                    ?: CCDToken.DECIMALS

            val amountSymbol =
                (ta.tokenSymbol
                    ?: CCDToken.SYMBOL)

            totalTextView.text =
                CurrencyUtil.formatGTU(
                    value = amountValue,
                    decimals = amountDecimals,
                ) + " $amountSymbol"

            totalTextView.setTextColor(
                if (amountValue.signum() > 0)
                    ContextCompat.getColor(
                        totalTextView.context,
                        R.color.mw24_green
                    )
                else
                    ContextCompat.getColor(
                        totalTextView.context,
                        R.color.cryptox_white_main
                    )
            )
        }

        // Cost
        when {
            (ta.isEncryptedTransfer() && ta.isOriginSelf()) -> {
                costTextView.visibility = View.VISIBLE
                costTextView.text =
                    costTextView.context.getString(R.string.account_details_shielded_transaction_fee)
            }

            (ta.cost != null && (ta.tokenTransferAmount != null || ta.subtotal != null)) -> {
                costTextView.visibility = View.VISIBLE

                var prefix = ""
                if (ta.status == TransactionStatus.RECEIVED ||
                    (ta.status == TransactionStatus.COMMITTED && ta.outcome == TransactionOutcome.Ambiguous)
                    || ta.status == TransactionStatus.ABSENT
                ) {
                    prefix = "~"
                }

                costTextView.text =
                    if (ta.cost.signum() == 0)
                        costTextView.context.getString(R.string.account_details_free_transaction)
                    else
                        costTextView.context.getString(R.string.account_details_fee) +
                                " $prefix" +
                                CurrencyUtil.formatGTU(ta.cost) +
                                " ${CCDToken.SYMBOL}"
            }

            ta.fromAddress != null -> {
                costTextView.visibility = View.VISIBLE
                costTextView.text = costTextView.context.getString(
                    R.string.transactions_from_address,
                    Account.getDefaultName(ta.fromAddress)
                )
            }

            else -> {
                costTextView.visibility = View.GONE
            }
        }
    }
}
