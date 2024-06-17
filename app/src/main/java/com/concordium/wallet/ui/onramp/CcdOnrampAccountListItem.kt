package com.concordium.wallet.ui.onramp

import com.concordium.wallet.data.room.AccountWithIdentity
import java.math.BigInteger

class CcdOnrampAccountListItem(
    val accountName: String,
    val identityName: String,
    val balance: BigInteger,
    val isDividerVisible: Boolean,
    val source: AccountWithIdentity?,
) {
    constructor(
        source: AccountWithIdentity,
        isDividerVisible: Boolean,
    ) : this(
        accountName = source.account.getAccountName(),
        identityName = source.identity.name,
        balance = source.account.getAtDisposalWithoutStakedOrScheduled(source.account.totalUnshieldedBalance),
        isDividerVisible = isDividerVisible,
        source = source,
    )
}
