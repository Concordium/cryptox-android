package com.concordium.wallet.data.backend.airdrop

import com.concordium.wallet.data.backend.common.ResponseMeta
import com.google.gson.annotations.SerializedName

data class RegistrationResponse(
    @SerializedName("response_meta")
    val responseMeta: ResponseMeta,
    val success: Boolean,
    val message: String
)