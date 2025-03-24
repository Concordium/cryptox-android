package com.concordium.wallet.ui.bakerdelegation.common.segmentedview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.R
import com.concordium.wallet.databinding.SegmentedTextItemBinding

class SegmentedButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs), SegmentedView {

    private val binding: SegmentedTextItemBinding

    init {
        LayoutInflater.from(context).inflate(R.layout.segmented_text_item, this, true)
        binding = SegmentedTextItemBinding.bind(this)
    }

    override fun setLayout(title: String, earningPercent: String, selected: Boolean) {
        binding.button.text = title
        binding.button.isSelected = selected
    }

    override fun onCheck(selected: Boolean) {
        binding.button.isSelected = selected
    }
}