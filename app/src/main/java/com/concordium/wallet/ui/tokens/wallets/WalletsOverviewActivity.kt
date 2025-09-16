package com.concordium.wallet.ui.tokens.wallets

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.Constants.Extras.EXTRA_PROVIDER_DATA
import com.concordium.wallet.Constants.Extras.EXTRA_WALLET_DATA
import com.concordium.wallet.R
import com.concordium.wallet.data.model.WalletMeta
import com.concordium.wallet.databinding.ActivityWalletsOverviewBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.tokens.provider.ProviderMeta
import com.concordium.wallet.ui.tokens.tokens.TokensOverviewActivity

class WalletsOverviewActivity : BaseActivity(R.layout.activity_wallets_overview),
    WalletItemView.IWalletItemView {

    private lateinit var sharedViewModel: WalletsOverviewViewModel
    private val binding by lazy {
        ActivityWalletsOverviewBinding.bind(findViewById(R.id.root_layout))
    }

    private var walletsPool: RecyclerView? = null
    private var walletsAdapter = WalletsAdapter(this)

    private var providerData: ProviderMeta? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val providerData = intent.getParcelableExtra<ProviderMeta>(EXTRA_PROVIDER_DATA)
        if (providerData == null) {
            finish()
            return
        }

        this.providerData = providerData

        initializeViews()
        setActionBarTitle(providerData.name)
        hideQrScan(false)
        hideLeftPlus(false)
        hideActionBarBack(isVisible = true) {
            finish()
        }
        initializeViewModel()
    }

    private fun initializeViewModel() {
        sharedViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        )[WalletsOverviewViewModel::class.java]
    }

    private fun showWallets() {
        val w = providerData?.wallets?.filter { it.total > 0 } ?: emptyList()
        walletsAdapter.setData(w)
        walletsAdapter.notifyDataSetChanged()
        showWaiting(false)
    }

    private fun initializeViews() {
        showWaiting(true)
        walletsPool = findViewById(R.id.wallets_pool)
        walletsPool?.setHasFixedSize(true)
        walletsPool?.layoutManager = LinearLayoutManager(applicationContext)
        walletsPool?.adapter = walletsAdapter

        showWallets()
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    override fun onClick(wallet: WalletMeta) {
        val w = wallet.copy(website = providerData?.website)
        Intent(applicationContext, TokensOverviewActivity::class.java).apply {
            putExtra(EXTRA_WALLET_DATA, w)
            startActivity(this)
        }
    }

    //endregion
}
