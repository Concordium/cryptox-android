package com.concordium.wallet.data.backend.wert

import com.google.gson.annotations.SerializedName

data class WertSessionResponse(
    @SerializedName("requestId")
    val requestId: String,
    @SerializedName("sessionId")
    val sessionId: String
)
