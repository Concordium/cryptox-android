package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.Account
import java.math.BigInteger

data class CCDToken(
    override val balance: BigInteger,
    override val accountAddress: String,
    var isEarning: Boolean,
    val eurPerMicroCcd: SimpleFraction?,
) : Token {

    constructor(
        account: Account,
        eurPerMicroCcd: SimpleFraction?,
    ) : this(
        balance = account.balanceAtDisposal,
        accountAddress = account.address,
        isEarning = account.isBaking() || account.isDelegating(),
        eurPerMicroCcd = eurPerMicroCcd,
    )

    override val isNewlyReceived: Boolean = false

    override val addedAt: Long = 0L

    override var isSelected: Boolean = false

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
