package com.concordium.wallet.ui.more.unshielding

import java.io.Serializable
import java.math.BigInteger

data class UnshieldingResult(
    val accountAddress: String,
    val unshieldedAmount: BigInteger,
) : Serializable
