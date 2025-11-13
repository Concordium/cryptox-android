package com.concordium.wallet.ui.cis2.send

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.ActivitySelectTokenBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.TokensAccountDetailsAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SelectTokenActivity : BaseActivity(
    R.layout.activity_select_token,
    R.string.cis_select_token_title
) {

    private val binding by lazy {
        ActivitySelectTokenBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private val viewModel: SelectTokenViewModel by viewModel {
        parametersOf(
            requireNotNull(intent.getStringExtra(EXTRA_ACCOUNT_ADDRESS)) {
                "Missing $EXTRA_ACCOUNT_ADDRESS extra"
            }
        )
    }

    private lateinit var tokensAccountDetailsAdapter: TokensAccountDetailsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        initViews()
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

            override fun onRowClick(token: Token) {
                goBackWithToken(token)
            }
        })
    }

    private fun initObservers() {
        viewModel.tokenList.collectWhenStarted(
            this,
            tokensAccountDetailsAdapter::setData
        )

        viewModel.isLoading.collectWhenStarted(
            this,
            binding.loading.progressBar::isVisible::set
        )

        viewModel.errorRes.collectWhenStarted(this) { errorRes ->
            binding.errorTextView.isVisible = errorRes != null
            binding.errorTextView.text = errorRes?.let(::getString)
        }
    }

    private fun goBackWithToken(token: Token) {
        setResult(
            RESULT_OK,
            Intent().putExtra(EXTRA_SELECTED_TOKEN, token)
        )
        finish()
    }

    companion object {
        const val EXTRA_ACCOUNT_ADDRESS = "select_token_account_address"
        const val EXTRA_SELECTED_TOKEN = "select_token_token"
    }
}
