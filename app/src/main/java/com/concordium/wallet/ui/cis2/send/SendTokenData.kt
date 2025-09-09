package com.concordium.wallet.ui.cis2.send

import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import java.io.Serializable
import java.math.BigInteger

data class SendTokenData(
    val account: Account,
    var token: Token,
    var amount: BigInteger = BigInteger.ZERO,
    var maxAmount: BigInteger? = null,
    var receiverAddress: String? = null,
    var receiverName: String? = null,
    var memoHex: String? = null,
    var fee: BigInteger? = null,
    var accountNonce: AccountNonce? = null,
    var maxEnergy: Long? = null,
    var expiry: Long = 0L,
) : Serializable
