package com.concordium.wallet.ui.onramp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityCcdOnrampSitesBinding
import com.concordium.wallet.ui.base.BaseActivity

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
        setActionBarTitle("")
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

        if (site.type == CcdOnrampSite.Type.DEX) {
            OpenCcdOnrampSiteWithAccountUseCase(
                site = site,
                accountAddress = "",
                onAccountAddressCopied = { },
                context = this,
            ).invoke()
        } else {
            if (accountAddress != null) {
                OpenCcdOnrampSiteWithAccountUseCase(
                    site = site,
                    accountAddress = accountAddress,
                    onAccountAddressCopied = {
                        Toast
                            .makeText(
                                this,
                                getString(R.string.template_ccd_onramp_opening_site, site.name),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    },
                    context = this,
                ).invoke()
            } else {
                val intent = Intent(this, CcdOnrampAccountsActivity::class.java)
                intent.putExtras(
                    CcdOnrampAccountsActivity.getBundle(
                        site = site,
                    )
                )
                startActivity(intent)
            }
        }
    }

    companion object {
        private const val ACCOUNT_ADDRESS_EXTRA = "account_address"

        fun getBundle(accountAddress: String?) = Bundle().apply {
            putString(ACCOUNT_ADDRESS_EXTRA, accountAddress)
        }
    }
}
