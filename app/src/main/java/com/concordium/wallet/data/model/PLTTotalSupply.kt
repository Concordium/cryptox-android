package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

data class PLTTotalSupply(
    val decimals: Int,
    val value: BigInteger
) : Serializable
