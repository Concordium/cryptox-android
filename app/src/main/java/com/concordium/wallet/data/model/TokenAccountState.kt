package com.concordium.wallet.data.model

import java.math.BigInteger

data class TokenAccountState(
    val balance: TokenAccountStateBalance? = null,
    val state: TokenAccountStateList? = null,
)

data class TokenAccountStateBalance(
    val decimals: Int = 0,
    val value: BigInteger = BigInteger.ZERO
)

data class TokenAccountStateList(
    val allowList: Boolean? = null,
    val denyList: Boolean? = null,
)
