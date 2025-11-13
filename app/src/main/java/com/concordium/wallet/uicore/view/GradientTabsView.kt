package com.concordium.wallet.uicore.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.forEach
import com.concordium.wallet.R

class GradientTabsView : LinearLayout {

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initialize(context: Context) {
        if (isInEditMode) {
            addControl(
                label = "Send",
                icon = context.getDrawable(R.drawable.mw24_ic_send_btn),
                clickListener = null,
                initiallySelected = false
            )
            addControl(
                label = "Receive",
                icon = context.getDrawable(R.drawable.mw24_ic_receive_btn),
                clickListener = null,
                initiallySelected = true
            )
        }
    }

    fun clearAll() {
        removeAllViews()
    }

    fun addControl(
        label: String,
        icon: Drawable?,
        clickListener: OnItemClickListener?,
        initiallySelected: Boolean
    ): View {
        val view = GradientTabItemView(context)

        view.setTab(label, icon, initiallySelected)
        view.onCheck(initiallySelected)
        view.setOnClickListener {
            selectItem(it as GradientTabItemView)
            clickListener?.onItemClicked()
        }
        addView(view, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        return view
    }

    private fun selectItem(item: GradientTabItemView) {
        forEach { child ->
            child as GradientTabItemView
            child.onCheck(child == item)
        }
    }

    interface OnItemClickListener {
        fun onItemClicked()
    }
}