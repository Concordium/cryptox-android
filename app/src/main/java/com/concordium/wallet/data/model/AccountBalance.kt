package com.concordium.wallet.data.model

import java.io.Serializable

data class AccountBalance(
    val finalizedBalance: AccountBalanceInfo?
) : Serializable {
    fun accountExists(): Boolean =
        finalizedBalance != null
}
