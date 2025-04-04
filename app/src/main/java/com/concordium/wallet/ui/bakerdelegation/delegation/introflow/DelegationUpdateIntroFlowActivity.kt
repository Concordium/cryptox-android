package com.concordium.wallet.ui.bakerdelegation.delegation.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationRegisterAmountActivity

class DelegationUpdateIntroFlowActivity :
    BaseDelegationBakerFlowActivity(R.string.delegation_update_delegation_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(
            R.string.delegation_update_subtitle1,
            R.string.delegation_update_subtitle2,
            R.string.delegation_update_subtitle3
        )
    }

    override fun getButtonText(): String = getString(R.string.delegation_update_delegation_button)

    override fun isButtonEnabled(): Boolean = true

    override fun gotoContinue() {
        val intent = Intent(this, DelegationRegisterAmountActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }

    override fun getLink(position: Int): String {
        return "delegation_update_flow_en_" + (position + 1) + ".html"
    }
}
