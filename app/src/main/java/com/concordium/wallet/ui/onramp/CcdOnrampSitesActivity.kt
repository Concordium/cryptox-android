package com.concordium.wallet.ui.onramp

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
    private lateinit var viewModel: CcdOnrampSitesViewModel
    private val accountAddress: String? by lazy {
        intent.getStringExtra(ACCOUNT_ADDRESS_EXTRA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModel()
        initList()

        hideActionBarBack(isVisible = true)
        setActionBarTitle("")
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
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
        val accountAddress = this.accountAddress
        if (accountAddress != null) {
            OpenCcdOnrampSiteWithAccountUseCase(
                site = site,
                accountAddress = accountAddress,
                onAccountAddressCopied = {
                    Toast
                        .makeText(this, R.string.ccd_onramp_address_copied, Toast.LENGTH_SHORT)
                        .show()
                },
                context = this,
            ).invoke()
        } else {
            // TODO Open accounts screen
        }
    }

    companion object {
        private const val ACCOUNT_ADDRESS_EXTRA = "account_address"

        fun getBundle(accountAddress: String) = Bundle().apply {
            putString(ACCOUNT_ADDRESS_EXTRA, accountAddress)
        }
    }
}
