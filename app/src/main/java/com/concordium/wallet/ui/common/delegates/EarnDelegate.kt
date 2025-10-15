package com.concordium.wallet.ui.common.delegates

import androidx.fragment.app.Fragment
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.earn.EarnInfoFragment
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusFragment
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationStatusFragment


interface EarnDelegate {
    fun launchEarn(
        account: Account,
        hasPendingDelegationTransactions: Boolean,
        hasPendingBakingTransactions: Boolean,
        launchFragment: (Fragment) -> Unit
    )
}

class EranDelegateImpl : EarnDelegate {
    override fun launchEarn(
        account: Account,
        hasPendingDelegationTransactions: Boolean,
        hasPendingBakingTransactions: Boolean,
        launchFragment: (Fragment) -> Unit
    ) {
        when {
            (account.delegation != null || hasPendingDelegationTransactions) -> {
                launchFragment(
                    DelegationStatusFragment.newInstance(
                        DelegationStatusFragment.setBundle(account)
                    )
                )
            }

            (account.baker != null || hasPendingBakingTransactions) -> {
                launchFragment(
                    BakerStatusFragment.newInstance(
                        BakerStatusFragment.setBundle(account)
                    )
                )
            }

            else -> launchFragment(
                EarnInfoFragment.newInstance(EarnInfoFragment.setBundle(account))
            )
        }
    }
}