package com.concordium.wallet.ui.account.accountdetails

import android.content.Context
import com.concordium.wallet.R
import com.concordium.wallet.data.model.RemoteTransaction
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOriginType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer

class TransactionMappingHelper(
    private val recipientList: List<Recipient>,
) {

    data class RecipientResult(
        val hasFoundRecipient: Boolean,
        val recipientOrAddress: String,
    )

    private fun findRecipientOrUseAddress(address: String): RecipientResult {
        for (recipient in recipientList) {
            if (recipient.address == address) {
                return RecipientResult(true, recipient.name)
            }
        }
        return RecipientResult(false, Account.getDefaultName(address))
    }

    fun addTitlesToTransaction(transaction: Transaction, transfer: Transfer, ctx: Context) {
        if (transaction.isDelegationTransfer()) {
            transaction.title = ctx.getString(R.string.account_delegation_pending)
        } else if (transaction.isBakerTransfer()) {
            transaction.title = ctx.getString(R.string.account_baking_pending)
        } else if (transaction.isSmartContractUpdate()) {
            transaction.title = ctx.getString(R.string.account_smart_contract_update_pending)
        } else if (transaction.isTokenUpdate()) {
            transaction.title = ctx.getString(R.string.account_token_update_pending)
        } else {
            transaction.title = findRecipientOrUseAddress(transfer.toAddress).recipientOrAddress
        }
    }

    fun addTitleToTransaction(
        transaction: Transaction,
        remoteTransaction: RemoteTransaction,
    ) {
        var address: String? = null
        val source = remoteTransaction.details.transferSource
        val destination = remoteTransaction.details.transferDestination
        if (source != null && destination != null) {
            address = when (remoteTransaction.origin.type) {
                TransactionOriginType.Self -> destination
                TransactionOriginType.Account -> source
                else -> null
            }
        }
        if (address != null) {
            transaction.title = findRecipientOrUseAddress(address).recipientOrAddress
        } else {
            transaction.title = remoteTransaction.details.description
        }
    }
}
