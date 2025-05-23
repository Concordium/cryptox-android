package com.concordium.wallet.ui.bakerdelegation.baker.introflow

import android.content.Intent
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.bakerdelegation.baker.BakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.baker.BakerRegistrationCloseActivity
import com.concordium.wallet.ui.bakerdelegation.baker.BakerUpdatePoolSettingsActivity
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerFlowActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel

class BakerUpdateIntroFlow :
    BaseDelegationBakerFlowActivity(R.string.baker_update_title) {

    override fun getTitles(): IntArray {
        return intArrayOf(
            R.string.baker_update_intro_subtitle1,
            R.string.baker_update_intro_subtitle2,
            R.string.baker_update_intro_subtitle3,
            R.string.baker_update_intro_subtitle4,
            R.string.baker_update_intro_subtitle5
        )
    }

    override fun getButtonText(): String = getString(R.string.baker_update_button)

    override fun isButtonEnabled(): Boolean = false

    override fun gotoContinue() {
        bakerDelegationData?.type?.let { type ->
            when (type) {
                UPDATE_BAKER_STAKE -> gotoUpdateBakerStake()
                UPDATE_BAKER_POOL -> gotoUpdatePoolSettings()
                UPDATE_BAKER_KEYS -> gotoUpdateBakerKeys()
            }
        }
    }

    override fun getLink(position: Int): String {
        return "baker_update_intro_flow_en_" + (position + 1) + ".html"
    }

    private fun gotoUpdateBakerStake() {
        val intent = Intent(this, BakerRegisterAmountActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }

    private fun gotoUpdatePoolSettings() {
        val intent = Intent(this, BakerUpdatePoolSettingsActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }

    private fun gotoUpdateBakerKeys() {
        val intent = Intent(this, BakerRegistrationCloseActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }
}
