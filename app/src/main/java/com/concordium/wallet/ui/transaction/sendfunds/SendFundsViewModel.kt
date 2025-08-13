package com.concordium.wallet.ui.transaction.sendfunds

import com.concordium.wallet.App
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.InputEncryptedAmount
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.util.toBigInteger
import java.math.BigInteger

object SendFundsViewModel {

    suspend fun calculateInputEncryptedAmount(
        accountBalance: AccountBalance,
        accountUpdater: AccountUpdater,
    ): InputEncryptedAmount {
        val aggEncryptedAmount =
            accountBalance.finalizedBalance?.let {
                var agg = it.accountEncryptedAmount.selfAmount
                it.accountEncryptedAmount.incomingAmounts.forEach {
                    if (accountUpdater.lookupMappedAmount(it) != null) {
                        agg = App.appCore.cryptoLibrary.combineEncryptedAmounts(agg, it)
                            .toString()
                    }
                }
                agg
            } ?: ""

        val aggAmount = accountBalance.finalizedBalance?.let {
            var agg =
                accountUpdater.lookupMappedAmount(it.accountEncryptedAmount.selfAmount)
                    ?.toBigInteger()
                    ?: BigInteger.ZERO
            it.accountEncryptedAmount.incomingAmounts.forEach {
                agg += accountUpdater.lookupMappedAmount(it)?.toBigInteger() ?: BigInteger.ZERO
            }
            agg
        } ?: ""

        val index = accountBalance.finalizedBalance?.let {
            it.accountEncryptedAmount.startIndex + it.accountEncryptedAmount.incomingAmounts.count {
                accountUpdater.lookupMappedAmount(it) != null
            }
        } ?: 0

        return InputEncryptedAmount(aggEncryptedAmount, aggAmount.toString(), index)
    }
}
