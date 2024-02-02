package com.concordium.wallet.ui.welcome

import android.graphics.Color
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWelcomeTermsBinding
import com.concordium.wallet.ui.base.BaseActivity

class WelcomeTermsActivity: BaseActivity(
    R.layout.activity_welcome_terms,
    R.string.welcome_terms_title,
) {
    private val binding: ActivityWelcomeTermsBinding by lazy {
        ActivityWelcomeTermsBinding.bind(findViewById(R.id.root_layout))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(true)
        binding.toolbarLayout.toolbarTitle.setTextAppearance(R.style.CCX_Typography_PageTitle)

        with(binding.termsWebview) {
            isVerticalScrollBarEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
            loadUrl("file:///android_asset/terms.html")
        }
    }

    override fun loggedOut() {
        // Can't log in here.
    }
}
