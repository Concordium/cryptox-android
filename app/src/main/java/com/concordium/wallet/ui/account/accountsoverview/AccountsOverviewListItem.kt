package com.concordium.wallet.ui.account.accountsoverview

import com.concordium.wallet.data.room.AccountWithIdentity

sealed interface AccountsOverviewListItem {
    object CcdOnrampBanner: AccountsOverviewListItem

    class Account(
        val accountWithIdentity: AccountWithIdentity,
    ): AccountsOverviewListItem
}
