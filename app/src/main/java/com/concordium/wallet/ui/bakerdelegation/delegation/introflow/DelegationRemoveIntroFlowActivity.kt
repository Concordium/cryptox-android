package com.concordium.wallet.ui.bakerdelegation.delegation.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationRemoveActivity

class DelegationRemoveIntroFlowActivity :
    BaseDelegationBakerFlowActivity(R.string.delegation_intro_flow_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(
            R.string.delegation_remove_subtitle1,
        )
    }

    override fun gotoContinue() {
        val intent = Intent(this, DelegationRemoveActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(AccountDetailsActivity::class.java.canonicalName)
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/delegation_remove_flow_en_" + (position + 1) + ".html"
    }
}
