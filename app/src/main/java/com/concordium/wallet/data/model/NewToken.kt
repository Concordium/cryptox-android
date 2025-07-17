package com.concordium.wallet.data.model

import java.math.BigInteger

interface NewToken {
    val balance: BigInteger
    val accountAddress: String
    val isNewlyReceived: Boolean
    val addedAt: Long
    val metadata: TokenMetadata?
    val isSelected: Boolean
}