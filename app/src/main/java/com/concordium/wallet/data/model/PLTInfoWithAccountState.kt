package com.concordium.wallet.data.model

data class PLTInfoWithAccountState(
    val token: PLTInfo,
    val tokenAccountState: TokenAccountState? = null
)
