package com.concordium.wallet.ui.cis2.manage

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityManageTokenListBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.HidingTokenDialog
import com.concordium.wallet.ui.tokenmanager.ManageTokenListViewModel
import com.concordium.wallet.uicore.toast.showGradientToast
import com.concordium.wallet.util.getSerializable

class ManageTokenListActivity : BaseActivity(
    R.layout.activity_manage_token_list,
    R.string.cis_manage_token_list
) {
    private val binding by lazy {
        ActivityManageTokenListBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var manageTokenListViewModel: ManageTokenListViewModel
    private lateinit var tokensAdapter: ManageTokensListAdapter

    private lateinit var account: Account
    private var listUpdated = false

    companion object {
        const val ACCOUNT = "ACCOUNT"
        const val LIST_UPDATED = "list_updated"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)

        initViews()
        initViewModel()
        initFragmentListener()
    }

    override fun onResume() {
        super.onResume()
        manageTokenListViewModel.loadTokens(account.address)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        listUpdated = intent?.getBooleanExtra(LIST_UPDATED, false) == true
        if (listUpdated) {
            showToast()
        }
    }

    private fun initViewModel() {
        manageTokenListViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[ManageTokenListViewModel::class.java]

        account = intent.getSerializable(ACCOUNT, Account::class.java)

        manageTokenListViewModel.uiState.collectWhenStarted(this) { uiState ->
            binding.progress.progressBar.isVisible = uiState.loading
            tokensAdapter.setData(uiState.tokens)
            binding.tokensList.isVisible = uiState.tokens.isNotEmpty()
            uiState.error?.let {
                showError(it)
            }

            val isEmptyViewVisible = !uiState.loading && uiState.tokens.isEmpty()
            binding.emptyViewLayout.isVisible = isEmptyViewVisible
            hideAddContact(isVisible = isEmptyViewVisible.not()) {
                goToAddTokens()
            }
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
        tokensAdapter = ManageTokensListAdapter()
        binding.tokensList.adapter = tokensAdapter
        tokensAdapter.setTokenClickListener(object : ManageTokensListAdapter.TokenClickListener {
            override fun onHideClick(token: Token) {
                onHideTokenClicked(token)
            }
        })

        listUpdated = intent.getBooleanExtra(LIST_UPDATED, false)
        if (listUpdated) {
            showToast()
        }
        binding.emptyViewButton.setOnClickListener {
            goToAddTokens()
        }
    }

    private fun onHideTokenClicked(token: Token) {
        manageTokenListViewModel.selectToken(token)
        HidingTokenDialog.newInstance(
            HidingTokenDialog.getBundle(manageTokenListViewModel.selectedTokenSymbol())
        ).showSingle(supportFragmentManager, HidingTokenDialog.TAG)
    }

    private fun onHideToken() {
        manageTokenListViewModel.deleteSelectedToken(account.address)
        manageTokenListViewModel.loadTokens(account.address)
        showToast()
    }

    private fun goToAddTokens() {
        val intent = Intent(this, AddTokenActivity::class.java)
        intent.putExtra(ACCOUNT, account)
        startActivity(intent)
    }

    private fun showToast() {
        showGradientToast(
            R.drawable.mw24_ic_address_copy_check,
            getString(R.string.cis_tokens_updated)
        )
    }
}
