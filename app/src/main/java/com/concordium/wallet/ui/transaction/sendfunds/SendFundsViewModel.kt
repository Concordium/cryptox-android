package com.concordium.wallet.ui.transaction.sendfunds

import com.concordium.wallet.App
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.InputEncryptedAmount
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.util.toBigInteger
import java.math.BigInteger

object SendFundsViewModel {
    suspend fun calculateInputEncryptedAmount(
        accountId: Int,
        accountBalance: AccountBalance,
        transferRepository: TransferRepository,
        accountUpdater: AccountUpdater,
    ): InputEncryptedAmount {
        val lastNonceToInclude = accountBalance.finalizedBalance?.accountNonce ?: -2

        val allTransfers = transferRepository.getAllByAccountId(accountId)
        val unfinalisedTransfers = allTransfers.filter {
            it.transactionStatus != TransactionStatus.FINALIZED && (it.nonce?.nonce
                ?: -1) >= lastNonceToInclude
        }

        val aggEncryptedAmount = if (unfinalisedTransfers.isNotEmpty()) {
            val lastTransaction =
                unfinalisedTransfers.maxWithOrNull { a, b -> a.id.compareTo(b.id) }
            if (lastTransaction != null) {
                accountBalance.finalizedBalance?.let {
                    val incomingAmounts = it.accountEncryptedAmount.incomingAmounts.filter {
                        accountUpdater.lookupMappedAmount(it) != null
                    }
                    var agg = lastTransaction.newSelfEncryptedAmount ?: ""
                    for (i in lastTransaction.newStartIndex until it.accountEncryptedAmount.startIndex + incomingAmounts.count()) {
                        agg = App.appCore.cryptoLibrary.combineEncryptedAmounts(
                            agg,
                            incomingAmounts[i]
                        ).toString()
                    }
                    agg
                } ?: ""
            } else {
                ""
            }
        } else {
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
        }

        val aggAmount = accountBalance.finalizedBalance?.let {
            var agg =
                accountUpdater.lookupMappedAmount(it.accountEncryptedAmount.selfAmount)
                    ?.toBigInteger()
                    ?: BigInteger.ZERO
            it.accountEncryptedAmount.incomingAmounts.forEach {
                agg += accountUpdater.lookupMappedAmount(it)?.toBigInteger() ?: BigInteger.ZERO
            }
            unfinalisedTransfers.forEach {
                agg -= it.amount
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
