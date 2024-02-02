package com.concordium.wallet.uicore.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ViewTransactionSubmittedHashBinding

/**
 * A row for submitted transaction hash, with the copy button.
 *
 * @see transactionHash
 */
class TransactionSubmittedHashView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {
    private val binding: ViewTransactionSubmittedHashBinding

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_transaction_submitted_hash, this, true)

        binding = ViewTransactionSubmittedHashBinding.bind(this)

        binding.copyHashButton.setOnClickListener {
            val clipboardManager: ClipboardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                context.getString(R.string.wallet_connect_transaction_submitted_hash),
                transactionHash
            )
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(
                context,
                R.string.wallet_connect_transaction_submitted_hash_copied,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var transactionHash: CharSequence
        get() = binding.hashTextView.text
        set(value) {
            binding.hashTextView.text = value
        }

    init {
        if (isInEditMode) {
            transactionHash =
                "ffe2be99f1ce2e282a975193658f5dc93feab2ff7928eba529672c4f8dc4c8c55230eb0c6514e326"
        }
    }
}
