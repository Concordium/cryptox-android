package com.concordium.wallet.ui.onramp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityCcdOnrampSitesBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.toast.showCustomToast

class CcdOnrampSitesActivity : BaseActivity(
    R.layout.activity_ccd_onramp_sites,
    R.string.ccd_onramp_title,
) {
    private val binding: ActivityCcdOnrampSitesBinding by lazy {
        ActivityCcdOnrampSitesBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: CcdOnrampSitesViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initialize(
            accountAddress = intent.getStringExtra(ACCOUNT_ADDRESS_EXTRA) ?: "",
        )
        initList()

        hideActionBarBack(isVisible = true)
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.homeOnrampScreen()
    }

    private fun initList() {
        val adapter = CcdOnrampItemAdapter(
            onSiteClicked = { item: CcdOnrampListItem.Site ->
                item.source?.also(::onSiteClicked)
            },
            onReadDisclaimerClicked = {
                CcdOnrampDisclaimerDialog().showSingle(
                    supportFragmentManager,
                    CcdOnrampDisclaimerDialog.TAG
                )
            },
            isDisclaimerAccepted = true
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(this, adapter::setData)
        viewModel.siteToOpen.collectWhenStarted(this) { site ->
            site?.let {
                openSite(
                    site = it.first,
                    copyToClipboard = it.second
                )
            }
        }
        viewModel.sessionLoading.collectWhenStarted(this) { isLoading ->
            showLoading(isLoading)
        }
        viewModel.error.collectWhenStarted(this) { error ->
            if (error != -1) {
                Toast.makeText(this, getString(error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onSiteClicked(site: CcdOnrampSite) {
        App.appCore.tracker.homeOnrampSiteClicked(siteName = site.name)

        viewModel.onSiteClicked(site)
    }

    private fun openSite(
        site: CcdOnrampSite,
        copyToClipboard: Boolean
    ) {
        if (copyToClipboard) {
            val clipboardManager: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                getString(R.string.account_details_address),
                viewModel.accountAddress,
            )
            showCustomToast(
                title = getString(
                    R.string.template_ccd_onramp_opening_site,
                    site.name
                )
            )
            clipboardManager.setPrimaryClip(clipData)
        }

        openSite(site)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.progressBar.isVisible = isLoading
    }

    private fun openSite(launchSite: CcdOnrampSite) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(launchSite.url))
        startActivity(Intent.createChooser(browserIntent, launchSite.name))
    }

    companion object {
        private const val ACCOUNT_ADDRESS_EXTRA = "account_address"

        fun getBundle(accountAddress: String?) = Bundle().apply {
            putString(ACCOUNT_ADDRESS_EXTRA, accountAddress)
        }
    }
}
