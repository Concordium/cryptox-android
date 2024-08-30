package com.concordium.wallet.ui.common.delegates

import android.content.Intent
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.earn.EarnInfoActivity
import com.concordium.wallet.ui.account.earn.EarnInfoActivity.Companion.EXTRA_ACCOUNT_DATA
import com.concordium.wallet.ui.bakerdelegation.baker.BakerStatusActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationStatusActivity
import com.concordium.wallet.ui.base.BaseActivity

interface EarnDelegate {
    fun gotoEarn(
        activity: BaseActivity,
        account: Account,
        hasPendingDelegationTransactions: Boolean,
        hasPendingBakingTransactions: Boolean
    )
}

class EarnDelegateImpl : EarnDelegate {
    override fun gotoEarn(
        activity: BaseActivity,
        account: Account,
        hasPendingDelegationTransactions: Boolean,
        hasPendingBakingTransactions: Boolean
    ) {
        val intent: Intent
        if (account.delegation != null || hasPendingDelegationTransactions) {
            intent = Intent(activity, DelegationStatusActivity::class.java)
            intent.putExtra(
                DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
                BakerDelegationData(
                    account,
                    isTransactionInProgress = hasPendingDelegationTransactions,
                    type = ProxyRepository.UPDATE_DELEGATION
                )
            )
        } else if (account.baker != null || hasPendingBakingTransactions) {
            intent = Intent(activity, BakerStatusActivity::class.java)
            intent.putExtra(
                DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
                BakerDelegationData(
                    account,
                    isTransactionInProgress = hasPendingBakingTransactions,
                    type = ProxyRepository.REGISTER_BAKER
                )
            )
        } else {
            intent = Intent(activity, EarnInfoActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT_DATA, account)
        }
        activity.runOnUiThread {
            activity.startActivityForResultAndHistoryCheck(intent)
        }
    }
}
