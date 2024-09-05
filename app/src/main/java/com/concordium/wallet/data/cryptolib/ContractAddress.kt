package com.concordium.wallet.data.cryptolib

import com.google.gson.annotations.SerializedName

data class ContractAddress(
    @SerializedName("index")
    val index: Int,
    @SerializedName("subindex")
    val subIndex: Int
)
