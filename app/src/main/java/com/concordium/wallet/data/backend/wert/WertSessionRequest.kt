package com.concordium.wallet.data.backend.wert

import com.google.gson.annotations.SerializedName

data class WertSessionRequest(
    @SerializedName("flow_type")
    val flowType: String = "simple",
    @SerializedName("wallet_address")
    val walletAddress: String,
    @SerializedName("currency")
    val currency: String = "USD",
    @SerializedName("commodity")
    val commodity: String = "CCD",
    @SerializedName("network")
    val network: String = "concordium"
)