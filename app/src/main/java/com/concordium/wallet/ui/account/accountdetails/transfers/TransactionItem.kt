package com.concordium.wallet.ui.account.accountdetails.transfers

import com.concordium.wallet.data.model.Transaction

class TransactionItem(
    var transaction: Transaction? = null,
    val isDividerVisible: Boolean = false,
) : AdapterItem {

    override fun getItemType(): AdapterItem.ItemType {
        return AdapterItem.ItemType.Item
    }
}
