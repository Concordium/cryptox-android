package com.concordium.wallet.ui.bakerdelegation.delegation.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.delegation.DelegationRegisterAmountActivity

class DelegationCreateIntroFlowActivity :
    BaseDelegationBakerFlowActivity(R.string.delegation_intro_flow_learn_about) {

    override fun getTitles(): IntArray {
        return intArrayOf(
            R.string.delegation_intro_subtitle1,
            R.string.delegation_intro_subtitle2,
            R.string.delegation_intro_subtitle3,
            R.string.delegation_intro_subtitle4,
            R.string.delegation_intro_subtitle5,
            R.string.delegation_intro_subtitle6,
            R.string.delegation_intro_subtitle7
        )
    }

    override fun getButtonText(): String = getString(R.string.start_staking_button)

    override fun isButtonEnabled(): Boolean = true

    override fun gotoContinue() {
        val intent = Intent(this, DelegationRegisterAmountActivity::class.java)
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }

    override fun getLink(position: Int): String {
        return "delegation_intro_flow_en_" + (position + 1) + ".html"
    }
}
