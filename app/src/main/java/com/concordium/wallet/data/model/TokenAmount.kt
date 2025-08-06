package com.concordium.wallet.data.model

import java.math.BigInteger

data class TokenAmount(
    val decimals: Int,
    val value: BigInteger,
)
