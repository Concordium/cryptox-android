package com.concordium.wallet.data.model

import java.math.BigInteger

data class TransactionCost(
    val energy: Long,
    val cost: BigInteger,
)
