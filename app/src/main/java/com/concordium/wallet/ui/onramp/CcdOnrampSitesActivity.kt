package com.concordium.wallet.ui.onramp

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityCcdOnrampSitesBinding
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
                openSite(
                    site = site,
                    accountAddress = accountAddress,
                    onAccountAddressCopied = {
                        showGradientToast(
                            iconResId = R.drawable.mw24_ic_address_copy_check,
                            title = getString(R.string.template_ccd_onramp_opening_site, site.name)
                        )
                    }
                )
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

    companion object {
        private const val ACCOUNT_ADDRESS_EXTRA = "account_address"

        fun getBundle(accountAddress: String?) = Bundle().apply {
            putString(ACCOUNT_ADDRESS_EXTRA, accountAddress)
        }
    }
}
