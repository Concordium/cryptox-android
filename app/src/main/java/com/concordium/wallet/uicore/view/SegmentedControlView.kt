package com.concordium.wallet.uicore.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.forEach
import com.concordium.wallet.R

class SegmentedControlView : LinearLayout {
    private var onItemClickListener: OnItemClickListener? = null

    constructor (context: Context) : super(context, null, R.style.CryptoX_SegmentedControl) {
        init(null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs, R.style.CryptoX_SegmentedControl) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun init(attrs: AttributeSet?) {
        if (isInEditMode) {
            addControl("My test 1", null, false)
            addControl("My test 2", null, true)
            addControl("My test 3", null, false)
        }
    }

    fun clearAll() {
        removeAllViews()
    }

    fun addControl(
        title: String,
        clickListener: OnItemClickListener?,
        initiallySelected: Boolean
    ): View {
        val view =
            LayoutInflater.from(context).inflate(R.layout.segmented_text_item, this, false) as Button
        view.text = title
        view.isSelected = initiallySelected
        view.setOnClickListener {
            selectItem(it as Button)
            clickListener?.onItemClicked()
        }
        addView(view)
        return view
    }

    fun selectItem(item: Button) {
        forEach { child ->
            child as Button
            child.isSelected = child == item
        }
    }

    //region OnItemClickListener
    //************************************************************

    interface OnItemClickListener {
        fun onItemClicked()
    }

    //endregion
}
