package com.concordium.wallet.data.model

import java.io.Serializable

class PLTState(
    val decimals: Int,
    val moduleState: PLTModuleState,
) : Serializable
