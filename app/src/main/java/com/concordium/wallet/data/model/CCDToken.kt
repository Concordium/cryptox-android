package com.concordium.wallet.data.model

import java.math.BigInteger

data class CCDToken(
    override val balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String,
    override val isNewlyReceived: Boolean = false,
    override val addedAt: Long = 0L,
    override val isSelected: Boolean = false,
    var isEarning: Boolean = false,
    val eurPerMicroCcd: SimpleFraction? = null,
) : NewToken {

    override val metadata: TokenMetadata =
        TokenMetadata(
            symbol = "CCD",
            decimals = 6,
            unique = false,
            name = null,
            description = null,
            thumbnail = null,
            display = null,
            totalSupply = null
        )

    override val symbol: String =
        metadata.symbol!!
}
