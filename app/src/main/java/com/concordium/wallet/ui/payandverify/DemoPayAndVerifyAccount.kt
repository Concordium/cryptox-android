package com.concordium.wallet.ui.payandverify

import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import java.math.BigInteger

class DemoPayAndVerifyAccount(
    val account: Account,
    val identity: Identity,
    val balance: BigInteger,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DemoPayAndVerifyAccount) return false

        if (account.address != other.account.address) return false

        return true
    }

    override fun hashCode(): Int {
        return account.address.hashCode()
    }
}
