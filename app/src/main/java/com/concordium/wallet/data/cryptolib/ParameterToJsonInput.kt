package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.walletconnect.Schema

data class ParameterToJsonInput(
    val parameter: String,
    val schema: Schema,
    val schemaVersion: Int?,

    // Sometimes receive name doesn't matter,
    // but it must follow the format.
    val receiveName: String = "o.O",
)
