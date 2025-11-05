package com.concordium.wallet.uicore.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.forEach
import com.concordium.wallet.R
import com.concordium.wallet.ui.bakerdelegation.common.segmentedview.SegmentedButtonView
import com.concordium.wallet.ui.bakerdelegation.common.segmentedview.SegmentedLayoutView
import com.concordium.wallet.ui.bakerdelegation.common.segmentedview.SegmentedView

class SegmentedControlView : LinearLayout {

    constructor (context: Context) : super(context, null) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {
        if (isInEditMode) {
            addControl(
                title = "My test 1",
                clickListener = null,
                earningPercent = "",
                isButtonControl = false,
                initiallySelected = false
            )
            addControl(
                title = "My test 2",
                clickListener = null,
                earningPercent = "",
                isButtonControl = false,
                initiallySelected = false
            )
            addControl(
                title = "My test 3",
                clickListener = null,
                earningPercent = "",
                isButtonControl = false,
                initiallySelected = false
            )
        }
    }

    fun clearAll() {
        removeAllViews()
    }

    fun addControl(
        title: String,
        description: String = "",
        clickListener: OnItemClickListener?,
        earningPercent: String = "",
        isButtonControl: Boolean = false,
        initiallySelected: Boolean
    ): View {
        val view = if (isButtonControl) {
            LayoutInflater.from(context).inflate(
                R.layout.segmented_button_view,
                this,
                false
            ) as SegmentedButtonView
        } else {
            LayoutInflater.from(context).inflate(
                R.layout.segmented_layout_view,
                this,
                false
            ) as SegmentedLayoutView
        }
        view.setLayout(
            title = title,
            description = description,
            earningPercent = earningPercent,
            selected = initiallySelected
        )
        view.onCheck(initiallySelected)
        view.setOnClickListener {
            selectItem(it as SegmentedView)
            clickListener?.onItemClicked()
        }
        addView(view)
        return view
    }

    private fun selectItem(item: SegmentedView) {
        forEach { child ->
            child as SegmentedView
            child.onCheck(child == item)
        }
    }

    //region OnItemClickListener
    //************************************************************

    interface OnItemClickListener {
        fun onItemClicked()
    }

    //endregion
}
