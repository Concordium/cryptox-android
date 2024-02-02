package com.concordium.wallet.ui.more.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityAboutBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.handleUrlClicks

class AboutActivity : BaseActivity(
    R.layout.activity_about,
    R.string.about_title
) {

    private val binding by lazy {
        ActivityAboutBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.aboutContactText.handleUrlClicks { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(this, browserIntent, null)
        }

        binding.aboutSupportText.handleUrlClicks { url ->
            val emailIntent = Intent(Intent.ACTION_SENDTO)
            emailIntent.data = Uri.parse("mailto:$url")
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(url))
            try {
                // start email intent
                startActivity(Intent.createChooser(emailIntent, ""))
            } catch (e: Exception) {
                // Left empty on purpose
            }
        }

        binding.aboutVersionText.text = BuildConfig.VERSION_NAME

        hideActionBarBack(isVisible = true)
        binding.toolbarLayout.toolbarTitle.setTextAppearance(R.style.CCX_Typography_PageTitle)
    }

    //endregion

    override fun loggedOut() {
        // No need to show auth, there is no wallet-related logic on this screen.
    }
}
