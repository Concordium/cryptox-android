package com.concordium.wallet.data.model

import java.io.Serializable

data class PLTState(
    val decimals: Int,
    val moduleState: PLTModuleState,
    val tokenModuleRef: String,
    val totalSupply: PLTTotalSupply,
) : Serializable
