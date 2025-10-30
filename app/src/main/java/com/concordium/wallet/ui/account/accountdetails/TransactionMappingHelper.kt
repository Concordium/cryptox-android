package com.concordium.wallet.ui.account.accountdetails

import android.content.Context
import com.concordium.wallet.R
import com.concordium.wallet.data.model.RemoteTransaction
import com.concordium.wallet.data.model.Transaction

class TransactionMappingHelper() {

    fun addTitlesToTransaction(transaction: Transaction, ctx: Context) {
        if (transaction.isDelegationTransfer()) {
            transaction.title = ctx.getString(R.string.account_delegation_pending)
        } else if (transaction.isBakerTransfer()) {
            transaction.title = ctx.getString(R.string.account_baking_pending)
        } else if (transaction.isSmartContractUpdate()) {
            transaction.title = ctx.getString(R.string.account_smart_contract_update_pending)
        } else {
            transaction.title = ctx.getString(R.string.transaction_type_transfer)
        }
    }

    fun addTitleToTransaction(
        transaction: Transaction,
        remoteTransaction: RemoteTransaction,
        ctx: Context
    ) {
        transaction.title = when {
            transaction.isOriginSelf() && transaction.toAddress != null ||
                    !transaction.isOriginSelf() && transaction.fromAddress != null ->
                ctx.getString(R.string.transaction_type_transfer)

            else -> remoteTransaction.details.description
        }
    }
}
