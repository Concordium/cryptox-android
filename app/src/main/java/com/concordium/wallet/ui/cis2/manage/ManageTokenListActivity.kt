package com.concordium.wallet.ui.cis2.manage

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityManageTokenListBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.HidingTokenDialog
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.uicore.toast.showGradientToast
import com.concordium.wallet.util.getSerializable

class ManageTokenListActivity : BaseActivity(
    R.layout.activity_manage_token_list,
    R.string.cis_manage_token_list
) {
    private val binding by lazy {
        ActivityManageTokenListBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var viewModelTokens: TokensViewModel
    private lateinit var tokensAdapter: ManageTokensListAdapter

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val LIST_UPDATED = "list_updated"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        hideAddContact(isVisible = true) {
            goToAddTokens()
        }

        initViews()
        initViewModel()
        initFragmentListener()
    }

    private fun initViewModel() {
        viewModelTokens = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TokensViewModel::class.java]

        val account = intent.getSerializable(ACCOUNT, Account::class.java)

        viewModelTokens.tokenData.account = account
        viewModelTokens.loadTokens(account.address)

        viewModelTokens.waiting.observe(this) {
            tokensAdapter.setData(viewModelTokens.tokens)
            binding.progress.progressBar.isVisible = it
        }
    }

    private fun initFragmentListener() {
        supportFragmentManager.setFragmentResultListener(
            HidingTokenDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            val isHidingToken = HidingTokenDialog.getResult(bundle)
            if (isHidingToken) {
                onHideToken()
            }
        }
    }

    private fun initViews() {
        tokensAdapter = ManageTokensListAdapter(this)
        binding.tokensList.adapter = tokensAdapter
        tokensAdapter.setTokenClickListener(object : ManageTokensListAdapter.TokenClickListener {
            override fun onHideClick(token: Token) {
                onHideTokenClicked(token)
            }
        })
        val listUpdated = intent.getBooleanExtra(LIST_UPDATED, false)

        if (listUpdated) {
            showToast(showDescription = false)
        }
    }

    private fun onHideTokenClicked(token: Token) {
        viewModelTokens.tokenData.selectedToken = token
        viewModelTokens.tokenData.selectedToken?.symbol?.let {
            HidingTokenDialog.newInstance(
                HidingTokenDialog.getBundle(tokenName = it)
            ).showSingle(supportFragmentManager, HidingTokenDialog.TAG)
        }
    }

    private fun onHideToken() {
        viewModelTokens.deleteSelectedToken()
        viewModelTokens.loadTokens(viewModelTokens.tokenData.account?.address!!)
        showToast(showDescription = true)
    }

    private fun goToAddTokens() {
        val intent = Intent(this, AddTokenActivity::class.java)
        intent.putExtra(ACCOUNT, viewModelTokens.tokenData.account)
        startActivity(intent)
    }

    private fun showToast(showDescription: Boolean) {
        if (showDescription) {
            showGradientToast(
                R.drawable.mw24_ic_eye_close,
                getString(R.string.cis_tokens_updated),
                getString(
                    R.string.cis_tokens_updated_details,
                    viewModelTokens.tokenData.selectedToken?.symbol
                )
            )
        } else {
            showGradientToast(
                R.drawable.mw24_ic_address_copy_check,
                getString(R.string.cis_tokens_updated)
            )
        }
    }
}