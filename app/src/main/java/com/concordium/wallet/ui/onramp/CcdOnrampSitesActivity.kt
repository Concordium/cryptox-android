package com.concordium.wallet.ui.onramp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityCcdOnrampSitesBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.uicore.toast.showGradientToast

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
            accountAddress = intent.getStringExtra(ACCOUNT_ADDRESS_EXTRA),
        )
        initList()

        hideActionBarBack(isVisible = true)
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.homeOnrampScreen()
    }

    override fun onPause() {
        super.onPause()
        viewModel.clearWertSession()
    }

    private fun initList() {
        val adapter = CcdOnrampItemAdapter(
            onSiteClicked = { item: CcdOnrampListItem.Site ->
                item.source?.also(::onSiteClicked)
            },
            onReadDisclaimerClicked = {
                binding.recyclerview.smoothScrollToPosition(
                    viewModel.listItemsLiveData.value!!.indexOf(
                        CcdOnrampListItem.Disclaimer
                    )
                )
            }
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(this, adapter::setData)
        viewModel.wertSite.collectWhenStarted(this) { site ->
            site?.let {
                openSite(
                    site = it,
                    accountAddress = viewModel.accountAddress ?: ""
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
        val accountAddress = viewModel.accountAddress

        App.appCore.tracker.homeOnrampSiteClicked(
            siteName = site.name,
        )

        if (site.type == CcdOnrampSite.Type.DEX) {
            openSite(site = site)
        } else {
            if (accountAddress != null) {
                if (site.name == "Wert") {
                    viewModel.getWertSessionId(accountAddress)
                } else {
                    openSite(
                        site = site,
                        accountAddress = accountAddress,
                        onAccountAddressCopied = {
                            showGradientToast(
                                iconResId = R.drawable.mw24_ic_address_copy_check,
                                title = getString(
                                    R.string.template_ccd_onramp_opening_site,
                                    site.name
                                )
                            )
                        }
                    )
                }
            } else {
                val intent = Intent(this, CcdOnrampAccountsActivity::class.java)
                intent.putExtras(CcdOnrampAccountsActivity.getBundle(site = site))
                startActivity(intent)
            }
        }
    }

    private fun openSite(
        site: CcdOnrampSite,
        accountAddress: String = "",
        onAccountAddressCopied: () -> Unit = {}
    ) {
        OpenCcdOnrampSiteWithAccountUseCase(
            site = site,
            accountAddress = accountAddress,
            onAccountAddressCopied = onAccountAddressCopied,
            context = this
        ).invoke()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.progressBar.isVisible = isLoading
    }

    companion object {
        private const val ACCOUNT_ADDRESS_EXTRA = "account_address"

        fun getBundle(accountAddress: String?) = Bundle().apply {
            putString(ACCOUNT_ADDRESS_EXTRA, accountAddress)
        }
    }
}
