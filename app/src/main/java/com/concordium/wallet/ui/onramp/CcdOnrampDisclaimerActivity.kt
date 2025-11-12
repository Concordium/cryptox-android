package com.concordium.wallet.ui.onramp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityOnrampDisclaimerBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.handleUrlClicks
import com.concordium.wallet.util.IntentUtil
import com.concordium.wallet.util.getOptionalSerializable

class CcdOnrampDisclaimerActivity : BaseActivity(
    R.layout.activity_onramp_disclaimer,
    R.string.ccd_onramp_disclaimer_title
) {

    private lateinit var binding: ActivityOnrampDisclaimerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOnrampDisclaimerBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true) {
            goBackWithResult(false)
        }

        val isDisclaimerAccepted = intent.getBooleanExtra(IS_DISCLAIMER_ACCEPTED, false)
        val showSiteIcon = intent.getBooleanExtra(SHOW_SITE_ICON, false)

        if (isDisclaimerAccepted) {
            binding.proceedButton.isVisible = false
            binding.cancelButton.isVisible = false
            binding.closeButton.isVisible = true
        } else {
            binding.proceedButton.isVisible = true
            binding.cancelButton.isVisible = true
            binding.closeButton.isVisible = false
            if (showSiteIcon) {
                binding.proceedButton.setIconEndDrawable(
                    AppCompatResources.getDrawable(
                        this, R.drawable.mw24_ic_external_link
                    )
                )
            }
        }

        binding.detailsTextView.handleUrlClicks { url ->
            IntentUtil.openUrl(this, url)
        }

        listOf(binding.cancelButton, binding.closeButton).forEach {
            it.setOnClickListener {
                goBackWithResult(false)
            }
        }

        binding.proceedButton.setOnClickListener {
            val siteToOpen = intent.getOptionalSerializable(
                SITE_TO_OPEN,
                CcdOnrampSite::class.java
            )
            val copyAddress = intent.getBooleanExtra(
                COPY_ADDRESS,
                false
            )
            goBackWithResult(true, siteToOpen, copyAddress)
        }
    }

    private fun goBackWithResult(
        accepted: Boolean,
        siteToOpen: CcdOnrampSite? = null,
        copyAddress: Boolean = false
    ) {
        val intent = Intent().apply {
            putExtra(DISCLAIMER_ACCEPTED, accepted)
            putExtra(COPY_ADDRESS, copyAddress)
            siteToOpen?.let { putExtra(SITE_TO_OPEN, it) }
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        const val IS_DISCLAIMER_ACCEPTED = "is_disclaimer_accepted"
        const val SITE_TO_OPEN = "site_to_open"
        const val SHOW_SITE_ICON = "show_site_icon"
        const val DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        const val COPY_ADDRESS = "copy_address"
    }
}