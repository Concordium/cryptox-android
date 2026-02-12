package com.concordium.wallet.ui.more.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityAboutBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.welcome.WelcomeTermsActivity
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

        fun onUrlClicked(url: String) {
            App.appCore.tracker.aboutScreenLinkClicked(
                url = url,
            )

            when {
                url == "#terms" -> {
                    startActivity(Intent(this, WelcomeTermsActivity::class.java))
                }

                url.startsWith("mailto") -> {
                    val emailIntent = Intent(Intent.ACTION_SENDTO)
                    emailIntent.data = Uri.parse(url)
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(url))
                    try {
                        // start email intent
                        startActivity(
                            Intent.createChooser(
                                emailIntent,
                                getString(R.string.about_support)
                            )
                        )
                    } catch (e: Exception) {
                        // Left empty on purpose
                    }
                }

                else -> {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    ContextCompat.startActivity(this, browserIntent, null)
                }
            }
        }

        binding.aboutVersionText.text = buildString {
            append(BuildConfig.VERSION_NAME)
            append(" ")
            append(BuildConfig.FLAVOR)
            if (BuildConfig.DEBUG) {
                append(" (debug)")
            }
        }

        binding.aboutContactText.handleUrlClicks(::onUrlClicked)
        binding.aboutSupportText.handleUrlClicks(::onUrlClicked)
        binding.termsTextView.handleUrlClicks(::onUrlClicked)
        binding.privacyTextView.handleUrlClicks(::onUrlClicked)

        binding.telegramButton.setOnClickListener {
            openSocial(TELEGRAM_LINK)
        }
        binding.twitterButton.setOnClickListener {
            openSocial(TWITTER_LINK)
        }
        binding.discordButton.setOnClickListener {
            openSocial(DISCORD_LINK)
        }

        hideActionBarBack(isVisible = true)
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.aboutScreen()
    }
    //endregion

    override fun loggedOut() {
        // No need to show auth, there is no wallet-related logic on this screen.
    }

    private fun openSocial(link: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        ContextCompat.startActivity(this, browserIntent, null)
    }

    companion object {
        private const val TELEGRAM_LINK = "https://t.me/ConcordiumNews"
        private const val TWITTER_LINK = "https://x.com/ConcordiumNet"
        private const val DISCORD_LINK = "https://discord.com/invite/GpKGE2hCFx"
    }
}
