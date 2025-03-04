package com.concordium.wallet.ui.bakerdelegation.baker.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.bakerdelegation.baker.BakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel

class BakerRegistrationIntroFlow :
    BaseDelegationBakerFlowActivity(R.string.baker_intro_flow_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(
            R.string.baker_intro_subtitle1,
            R.string.baker_intro_subtitle2,
            R.string.baker_intro_subtitle3,
            R.string.baker_intro_subtitle4,
        )
    }

    override fun gotoContinue() {
        val intent = Intent(this, BakerRegisterAmountActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/baker_intro_flow_en_" + (position + 1) + ".html"
    }
}
