package com.concordium.wallet.ui.onramp.wert

import com.google.gson.annotations.SerializedName

data class WertCommodityInfo(
    @SerializedName("commodity")
    val commodity: String,
    @SerializedName("network")
    val network: String
)