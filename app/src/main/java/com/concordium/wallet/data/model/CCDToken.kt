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
        withTotalBalance: Boolean = false,
        eurPerMicroCcd: SimpleFraction? = null,
    ) : this(
        balance = if (withTotalBalance) account.balance else account.balanceAtDisposal,
        accountAddress = account.address,
        isEarning = account.isBaking() || account.isDelegating(),
        eurPerMicroCcd = eurPerMicroCcd,
    )

    override val isNewlyReceived: Boolean = false

    override val addedAt: Long = 0L

    override var isSelected: Boolean = false

    override val symbol: String = SYMBOL

    override val decimals: Int = DECIMALS

    companion object {
        const val SYMBOL = "CCD"
        const val DECIMALS = 6
    }
}
