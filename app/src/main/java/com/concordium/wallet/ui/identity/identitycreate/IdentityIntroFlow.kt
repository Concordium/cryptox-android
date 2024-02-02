package com.concordium.wallet.ui.identity.identitycreate

import android.content.Intent
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.common.GenericFlowActivity

class IdentityIntroFlow : GenericFlowActivity(R.string.identity_intro_flow_title) {
    companion object {
        val TITLES = intArrayOf(
            R.string.identity_intro_flow_subtitle1,
            R.string.identity_intro_flow_subtitle2,
            R.string.identity_intro_flow_subtitle3,
            R.string.identity_intro_flow_subtitle4
        )
        const val MAX_PAGES = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActionBarBack(isVisible = false)

        showProgressLine = true
        progressLineFilledDots = 3
        progressLineTotalDots = 5
        updateViews()
    }

    override fun onBackPressed() {
    }

    override fun gotoContinue() {
        finishAffinity()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(MainActivity.EXTRA_CREATE_FIRST_IDENTITY, true)
        startActivity(intent)
    }

    override fun getMaxPages(): Int {
        return MAX_PAGES
    }

    override fun getPageTitle(position: Int): Int {
        return TITLES[position]
    }

    override fun getLink(position: Int): String {
        return "file:///android_asset/identity_intro_flow_en_" + (position + 1) + ".html"
    }
}
