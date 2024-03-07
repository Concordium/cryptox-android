package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.walletconnect.AccountTransactionPayload

data class CreateAccountTransactionInput(
    val expiry: Long,
    val from: String,
    val keys: AccountData,
    val nonce: Int,
    val payload: AccountTransactionPayload,
    val type: String
)
