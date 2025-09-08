package com.concordium.wallet.data.model

class TokenAccountState(
    val balance: TokenAmount,
    val state: TokenAccountStateList,
)

class TokenAccountStateList(
    val allowList: Boolean? = null,
    val denyList: Boolean? = null,
    val paused: Boolean = false,
)
