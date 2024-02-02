package com.concordium.wallet.data.backend.tokens

import com.concordium.wallet.data.backend.common.ResponseMeta
import com.concordium.wallet.ui.tokens.provider.Token
import com.google.gson.annotations.SerializedName

data class TokensResponse(
    @SerializedName("response_meta")
    val responseMeta: ResponseMeta,
    @SerializedName("tokens")
    val tokens: List<Token>,
    @SerializedName("page_start")
    val pageStart: Int,
    @SerializedName("page_limit")
    val pageLimit: Int,
    @SerializedName("total_items")
    val total: Int
)