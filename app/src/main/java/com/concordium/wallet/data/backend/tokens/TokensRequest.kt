package com.concordium.wallet.data.backend.tokens

import com.google.gson.annotations.SerializedName

data class TokensRequest(
    @SerializedName("blockchain_id")
    val blockchainId: String = "2",
    @SerializedName("address")
    val address: String,
    @SerializedName("page_start")
    val pageStart: Int = 0,
    @SerializedName("page_limit")
    val pageLimit: Int = 26,
    @SerializedName("search_string")
    val searchString: String? = null
)