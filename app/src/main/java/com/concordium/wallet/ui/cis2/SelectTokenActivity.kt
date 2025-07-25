package com.concordium.wallet.ui.cis2

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.R
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivitySelectTokenBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable

class SelectTokenActivity : BaseActivity(
    R.layout.activity_select_token,
    R.string.cis_select_token_title
) {

    private val binding by lazy {
        ActivitySelectTokenBinding.bind(findViewById(R.id.root_layout))
    }

    private lateinit var viewModelSend: SendTokenViewModel
    private lateinit var viewModelTokens: ManageTokensViewModel
    private lateinit var tokensAccountDetailsAdapter: TokensAccountDetailsAdapter
    private lateinit var account: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        initViews()
        initViewModels()
    }

    private fun initViews() {
        binding.tokensList.layoutManager = LinearLayoutManager(this)
        tokensAccountDetailsAdapter = TokensAccountDetailsAdapter(
            context = this,
            showManageButton = false
        )
        tokensAccountDetailsAdapter.also { binding.tokensList.adapter = it }

        tokensAccountDetailsAdapter.setTokenClickListener(object :
            TokensAccountDetailsAdapter.TokenClickListener {
            override fun onRowClick(token: NewToken) {
//                goBackWithToken(token)
            }
        })
    }

    private fun initViewModels() {
        viewModelSend = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SendTokenViewModel::class.java]

        viewModelTokens = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[ManageTokensViewModel::class.java]

        account = intent.getSerializable(SELECT_TOKEN_ACCOUNT, Account::class.java)
        viewModelTokens.tokenData.account = account
        viewModelSend.loadTokens(account.address)

        viewModelSend.waiting.observe(this) { waiting ->
            showWaiting(waiting)
        }
        viewModelSend.tokens.observe(this) { tokens ->
            viewModelTokens.tokens = tokens as MutableList<Token>
//            viewModelTokens.loadTokensBalances()
        }
        viewModelTokens.tokenBalances.observe(this) { ready ->
            showWaiting(ready.not())
//            if (ready)
//                tokensAccountDetailsAdapter.setData(viewModelTokens.tokens)
        }
    }

    private fun showWaiting(waiting: Boolean) {
        binding.loading.progressBar.visibility = if (waiting) View.VISIBLE else View.GONE
    }

//    private fun goBackWithToken(token: Token) {
//        val intent = Intent().apply {
//            putExtra(SELECT_TOKEN_ACCOUNT, token)
//        }
//        setResult(Activity.RESULT_OK, intent)
//        finish()
//    }

    companion object {
        const val SELECT_TOKEN_ACCOUNT = "select_token_account"
    }

}