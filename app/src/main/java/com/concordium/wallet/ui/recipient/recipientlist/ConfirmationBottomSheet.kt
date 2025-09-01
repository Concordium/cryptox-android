package com.concordium.wallet.ui.recipient.recipientlist

import android.content.Context
import android.widget.Button
import android.widget.TextView
import com.concordium.wallet.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

interface INotification {
    fun confirmDeleteContact(data: Any?)
    fun cancelDeleteContact()
}

class ConfirmationBottomSheet(context: Context, callback: INotification) : BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme) {

    enum class Type {
        INFO, ERROR, WARNING
    }

    private val btnCancel: Button?
    private val btnDelete: Button?
    private val titleTv: TextView?
    private val descriptionTv: TextView?
    private var data: Any? = null

    init {
        setContentView(R.layout.confirmation_bottomsheet)
        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        behavior.skipCollapsed = true
        behavior.isFitToContents = false
        behavior.isDraggable = false

        titleTv = findViewById(R.id.title)
        descriptionTv = findViewById(R.id.description)
        btnCancel = findViewById(R.id.btnCancel)
        btnDelete = findViewById(R.id.btnDelete)

        btnCancel?.setOnClickListener {
            this.dismiss()
            callback.cancelDeleteContact()
        }
        btnDelete?.setOnClickListener {
            this.dismiss()
            callback.confirmDeleteContact(data)
        }
        setOnDismissListener {
            callback.cancelDeleteContact()
        }
    }

    fun setData(type: Type = Type.WARNING, description: String, data: Any? = null) {
        titleTv?.text = when (type) {
            Type.WARNING -> "Warning"
            Type.INFO -> "Information"
            Type.ERROR -> "Error"
        }
        descriptionTv?.text = description
        this.data = data
        show()
    }
}
