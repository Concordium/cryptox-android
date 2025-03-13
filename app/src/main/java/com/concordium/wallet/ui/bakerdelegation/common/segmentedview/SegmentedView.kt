package com.concordium.wallet.ui.bakerdelegation.common.segmentedview

interface SegmentedView {
    fun onCheck(selected: Boolean)
    fun setLayout(title: String, earningPercent: String, selected: Boolean)
}