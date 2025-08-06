package com.concordium.wallet.data.model

data class TokenAccountState(
    val balance: TokenAmount? = null,
    val state: TokenAccountStateList? = null,
)

data class TokenAccountStateList(
    val allowList: Boolean? = null,
    val denyList: Boolean? = null,
)
