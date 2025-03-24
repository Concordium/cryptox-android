package com.concordium.wallet.ui.bakerdelegation.common.segmentedview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.databinding.SegmentedLayoutItemBinding

class SegmentedLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs), SegmentedView {

    private val binding: SegmentedLayoutItemBinding

    init {
        LayoutInflater.from(context).inflate(R.layout.segmented_layout_item, this, true)
        binding = SegmentedLayoutItemBinding.bind(this)
    }

    override fun setLayout(title: String, earningPercent: String, selected: Boolean) {
        binding.title.text = title
        binding.earningPercent.apply {
            text = earningPercent
            isVisible = earningPercent.isNotEmpty()
        }
        binding.checkbox.isChecked = selected
    }

    override fun onCheck(selected: Boolean) {
        binding.checkbox.isChecked = selected
    }
}