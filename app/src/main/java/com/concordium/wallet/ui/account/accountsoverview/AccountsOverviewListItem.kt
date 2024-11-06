package com.concordium.wallet.ui.account.accountsoverview

import com.concordium.wallet.data.room.AccountWithIdentity

sealed interface AccountsOverviewListItem {
    class Account(
        val accountWithIdentity: AccountWithIdentity,
    ): AccountsOverviewListItem
}
