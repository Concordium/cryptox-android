package com.concordium.wallet.ui.payandverify

import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import java.math.BigInteger

class DemoPayAndVerifyAccount(
    val account: Account,
    val identity: Identity,
    val balance: BigInteger,
    val tokenSymbol: String,
    val tokenDecimals: Int,
)
