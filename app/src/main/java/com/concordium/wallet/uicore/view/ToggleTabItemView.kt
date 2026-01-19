package com.concordium.wallet.uicore.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ItemSegmentedTabBinding

class ToggleTabItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {

    private val binding: ItemSegmentedTabBinding

    init {
        LayoutInflater.from(context).inflate(R.layout.item_segmented_tab, this, true)
        binding = ItemSegmentedTabBinding.bind(this)
    }

    fun setTab(label: String, icon: Drawable?, selected: Boolean) {
        binding.tabText.text = label
        binding.root.isSelected = selected
        icon?.let(binding.tabIcon::setImageDrawable)
    }

    fun onCheck(selected: Boolean) {
        binding.root.isSelected = selected
    }
}