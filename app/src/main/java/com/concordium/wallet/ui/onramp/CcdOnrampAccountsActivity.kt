package com.concordium.wallet.ui.onramp

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityCcdOnrampAccountsBinding
import com.concordium.wallet.ui.base.BaseActivity

class CcdOnrampAccountsActivity : BaseActivity(
    R.layout.activity_ccd_onramp_accounts,
    R.string.ccd_onramp_accounts_title,
) {
    private val binding: ActivityCcdOnrampAccountsBinding by lazy {
        ActivityCcdOnrampAccountsBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: CcdOnrampAccountsViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    @Suppress("DEPRECATION")
    private val site: CcdOnrampSite by lazy {
        requireNotNull(intent.getSerializableExtra(SITE_EXTRA) as? CcdOnrampSite) {
            "No $SITE_EXTRA specified"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initList()

        hideActionBarBack(isVisible = true)
        setActionBarTitle("")
    }

    private fun initList() {
        val adapter = CcdOnrampAccountItemAdapter(
            onItemClicked = { item ->
                item.source?.account?.also(::onAccountClicked)
            }
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(this, adapter::setData)
    }

    private fun onAccountClicked(account: Account) {
        OpenCcdOnrampSiteWithAccountUseCase(
            site = site,
            accountAddress = account.address,
            onAccountAddressCopied = {
                Toast
                    .makeText(this, R.string.ccd_onramp_address_copied, Toast.LENGTH_SHORT)
                    .show()
            },
            context = this
        ).invoke()

        finish()
    }

    companion object {
        private const val SITE_EXTRA = "site"

        fun getBundle(site: CcdOnrampSite) = Bundle().apply {
            putSerializable(SITE_EXTRA, site)
        }
    }
}
