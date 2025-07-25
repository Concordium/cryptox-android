package com.concordium.wallet.ui.cis2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.R
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivitySelectTokenBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable

class SelectTokenActivity : BaseActivity(
    R.layout.activity_select_token,
    R.string.cis_select_token_title
) {

    private val binding by lazy {
        ActivitySelectTokenBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private val tokensListViewModel: TokensListViewModel by viewModels()

    private lateinit var tokensAccountDetailsAdapter: TokensAccountDetailsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        initViews()
        initViewModels()
        initObservers()
    }

    private fun initViews() {
        binding.tokensList.layoutManager = LinearLayoutManager(this)

        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(
            context = this,
            showManageButton = false,
        )
        binding.tokensList.adapter = tokensAccountDetailsAdapter

        tokensAccountDetailsAdapter.setTokenClickListener(object :
            TokensAccountDetailsAdapter.TokenClickListener {

            override fun onRowClick(token: NewToken) {
                goBackWithToken(token)
            }
        })
    }

    private fun initViewModels() {
        tokensListViewModel.loadTokens(
            account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java),
            onlyTransferable = true,
        )
    }

    private fun initObservers() {
        tokensListViewModel.uiState.collectWhenStarted(this) { state ->

            binding.loading.progressBar.isVisible = state.isLoading

            tokensAccountDetailsAdapter.setData(state.tokens)

            state.error?.contentOrNullIfUsed?.let(::showError)
        }
    }

    private fun goBackWithToken(token: NewToken) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(EXTRA_SELECTED_TOKEN, token)
        )
        finish()
    }

    companion object {
        const val EXTRA_ACCOUNT = "select_token_account"
        const val EXTRA_SELECTED_TOKEN = "select_token_token"
    }
}
