package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

data class GenesisHashResponse(
    @SerializedName("genesis_block")
    val genesisBlock: String,
)
