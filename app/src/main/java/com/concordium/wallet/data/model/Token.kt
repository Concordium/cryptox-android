package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

sealed interface Token : Serializable {
    val symbol: String
    val balance: BigInteger
    val accountAddress: String
    val isNewlyReceived: Boolean
    val addedAt: Long
    val metadata: TokenMetadata?
    var isSelected: Boolean

    val decimals: Int
        get() = metadata?.decimals ?: 0
}
