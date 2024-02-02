package com.concordium.wallet.ui.tokens.tokens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.data.model.WalletMeta
import com.concordium.wallet.databinding.ActivityTokensOverviewBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.tokens.provider.Token

class TokensOverviewActivity : BaseActivity(R.layout.activity_tokens_overview),
    TokenItemView.ITokenItemView {

    private lateinit var sharedViewModel: TokensOverviewViewModel
    private val binding by lazy {
        ActivityTokensOverviewBinding.bind(findViewById(R.id.root_layout))
    }
    private var tokensAdapter = TokensAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val walletMeta = intent.getParcelableExtra<WalletMeta>(Constants.Extras.EXTRA_WALLET_DATA)
        if (walletMeta == null) {
            finish()
            return
        } else {
            setActionBarTitle(walletMeta.name)
            initializeViews(walletMeta)
            hideActionBarBack(isVisible = true) {
                finish()
            }
            initializeViewModel(walletMeta)
        }
    }

    private fun initializeViewModel(walletMeta: WalletMeta) {
        sharedViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        )[TokensOverviewViewModel::class.java]

        sharedViewModel.onTokensReady().observe(this) { event ->
            event.contentOrNullIfUsed?.let { tokens ->
                tokensAdapter.setData(tokens, walletMeta.name)
                showWaiting(false)
            }
        }

        sharedViewModel.onNextTokensReady().observe(this) { event ->
            event.contentOrNullIfUsed?.let { tokens ->
                tokensAdapter.setNextData(tokens, walletMeta.name)
                showWaiting(false)
            }
        }

        sharedViewModel.processAction(TokenViewAction.GetTokens(walletMeta))
    }

    private fun initializeViews(walletMeta: WalletMeta) {
        val layoutManager = GridLayoutManager(applicationContext, 2)

        showWaiting(true)

        searchView?.setOnSearchClickListener {
            hideActionBarTitle(isVisible = false)
        }

        searchView?.setOnCloseListener {
            hideActionBarTitle(isVisible = true)
            false
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tokensAdapter.filter.filter(newText)
                return false
            }
        })

        binding.tokensPool.setHasFixedSize(false)
        binding.tokensPool.layoutManager = layoutManager
        binding.tokensPool.adapter = tokensAdapter
        binding.tokensPool.itemAnimator = null

        binding.tokensPool.addOnScrollListener(
            PaginationListener(
                layoutManager,
                object : PaginationListener.IPaginationCallback {
                    override fun onLoadMore(page: Int, count: Int) {
                        sharedViewModel.processAction(
                            TokenViewAction.GetNextTokens(
                                walletMeta,
                                count
                            )
                        )
                    }

                    override fun getTokensCount() = tokensAdapter.itemCount
                })
        )
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    override fun showFoundCount(count: Int) {
        if (count == 0) {
            binding.countTextView.text = getString(R.string.nft_nothing_found)
            binding.tokensPool.isVisible = false
        } else {
            binding.countTextView.text = resources.getQuantityString(
                R.plurals.nft_found_items_count,
                count,
                count
            )
            binding.tokensPool.isVisible = true
        }
    }

    override fun onClick(token: Token) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(token.nftPage)))
    }
}
