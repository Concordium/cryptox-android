package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

sealed interface Token : Serializable {
    val symbol: String
    val decimals: Int
    val balance: BigInteger
    val accountAddress: String
    val isNewlyReceived: Boolean
    val addedAt: Long
    var isSelected: Boolean
}
