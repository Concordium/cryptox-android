package com.concordium.wallet.data.walletconnect

import com.google.gson.annotations.SerializedName

data class TransactionSuccess(
    @SerializedName("hash")
    val hash: String
)
