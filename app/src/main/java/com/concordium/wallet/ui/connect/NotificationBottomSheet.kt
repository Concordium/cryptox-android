package com.concordium.wallet.ui.connect

import android.content.Context
import android.widget.TextView
import com.concordium.wallet.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class NotificationBottomSheet(context: Context, callback: ITransactionResult) : BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme) {

    enum class Type {
        INFO, ERROR, WARNING
    }

    private val titleTv: TextView?
    private val descriptionTv: TextView?

    init {
        setContentView(R.layout.notification_bottomsheet)
        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        behavior.skipCollapsed = true
        behavior.isFitToContents = false
        behavior.isDraggable = false

        titleTv = findViewById(R.id.title)
        descriptionTv = findViewById(R.id.description)

        setOnDismissListener {
            callback.onResultBottomSheetDismissed()
        }
    }

    fun setData(type: Type = Type.WARNING, description: String) {
        titleTv?.text = when (type) {
            Type.WARNING -> "Warning"
            Type.INFO -> "Information"
            Type.ERROR -> "Error"
        }
        descriptionTv?.text = description
        show()
    }
}
