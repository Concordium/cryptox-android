package com.concordium.wallet.ui.welcome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWelcomeBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.handleUrlClicks

class WelcomeActivity : BaseActivity(R.layout.activity_welcome) {
    private val binding: ActivityWelcomeBinding by lazy {
        ActivityWelcomeBinding.bind(findViewById(R.id.root_layout))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.consentTextView.handleUrlClicks { url ->
            when (url) {
                "#terms" ->
                    openTerms()

                else -> {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    ContextCompat.startActivity(this, browserIntent, null)
                }
            }
        }
        binding.consentCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                App.appCore.tracker.welcomeCheckBoxChecked()
            }
            binding.getStartedButton.isEnabled = isChecked
        }
        binding.getStartedButton.setOnClickListener {
            App.appCore.tracker.welcomeGetStartedClicked()
            goToStart()
        }

        // hasCompletedInitialSetup has positive value at the fresh app start,
        // hence we can skip this screen if the user has ever visited the next one.
        if (savedInstanceState == null && !App.appCore.session.hasCompletedInitialSetup) {
            goToStart()
        } else if (savedInstanceState == null){
            App.appCore.tracker.welcomeScreen()
        }
    }

    private fun goToStart() {
        finish()
        startActivity(Intent(this, WelcomePromoActivity::class.java))
    }

    private fun openTerms() {
        startActivity(Intent(this, WelcomeTermsActivity::class.java))
    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }
}
