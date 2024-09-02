package com.concordium.wallet.data.cryptolib

import com.google.gson.annotations.SerializedName

data class NotificationTokenMetadata(
    @SerializedName("url")
    val url: String,
    @SerializedName("hash")
    val hash: String?
)