package com.concordium.wallet.ui.transaction.transactiondetails

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ViewTransactionDetailsEntryBinding
import com.concordium.wallet.uicore.Formatter

class TransactionDetailsEntryView : ConstraintLayout {
    private lateinit var binding: ViewTransactionDetailsEntryBinding
    private var fullValue: String? = null

    constructor (context: Context) : super(context) {
        init(null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        LayoutInflater.from(context).inflate(R.layout.view_transaction_details_entry, this, true)
        binding = ViewTransactionDetailsEntryBinding.bind(this)

        val horizontalPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            20f,
            context.resources.displayMetrics,
        ).toInt()
        setPadding(
            horizontalPadding,
            0,
            horizontalPadding,
            0
        )

        if (attrs != null) {
            val ta =
                context.obtainStyledAttributes(attrs, R.styleable.TransactionDetailsEntryView, 0, 0)
            try {
                binding.titleTextview.setText(ta.getResourceIdOrThrow(R.styleable.TransactionDetailsEntryView_entry_title))
            } finally {
                ta.recycle()
            }
        }

        binding.copyImageview.visibility = View.GONE
    }

    fun setTitle(title: String) {
        binding.titleTextview.text = title
    }

    fun setValue(value: String, formatAsFirstEight: Boolean = false) {
        fullValue = value
        binding.valueTextview.text =
            if (formatAsFirstEight) Formatter.formatAsFirstEight(value) else value
    }

    fun setDivider(visible: Boolean = true) {
        binding.divider.isVisible = visible
    }
}
