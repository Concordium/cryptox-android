package com.concordium.wallet.ui.account.accountdetails

import android.content.Context
import com.concordium.wallet.R
import com.concordium.wallet.data.model.RemoteTransaction
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer

class TransactionMappingHelper(
    private val recipientList: List<Recipient>,
) {

    private fun getCounterpartyName(address: String): String {
        for (recipient in recipientList) {
            if (recipient.address == address) {
                return recipient.name
            }
        }
        return Account.getDefaultName(address)
    }

    fun addTitlesToTransaction(transaction: Transaction, transfer: Transfer, ctx: Context) {
        if (transaction.isDelegationTransfer()) {
            transaction.title = ctx.getString(R.string.account_delegation_pending)
        } else if (transaction.isBakerTransfer()) {
            transaction.title = ctx.getString(R.string.account_baking_pending)
        } else if (transaction.isSmartContractUpdate()) {
            transaction.title = ctx.getString(R.string.account_smart_contract_update_pending)
        } else {
            transaction.title = getCounterpartyName(transfer.toAddress)
        }
    }

    fun addTitleToTransaction(
        transaction: Transaction,
        remoteTransaction: RemoteTransaction,
    ) {
        transaction.title = when {
            transaction.isOriginSelf()
                    && transaction.toAddress != null ->
                getCounterpartyName(transaction.toAddress)

            !transaction.isOriginSelf()
                    && transaction.fromAddress != null ->
                getCounterpartyName(transaction.fromAddress)

            else ->
                remoteTransaction.details.description
        }
    }
}
