package com.concordium.wallet.ui.cis2.manage

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityManageTokenListBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.util.getSerializable

class ManageTokenListActivity : BaseActivity(
    R.layout.activity_manage_token_list,
    R.string.cis_manage_token_list
) {
    private val binding by lazy {
        ActivityManageTokenListBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var viewModelTokens: TokensViewModel

    companion object {
        const val ACCOUNT = "ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        hideAddContact(isVisible = true) {
            goToAddTokens()
        }

        viewModelTokens = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TokensViewModel::class.java]

        viewModelTokens.tokenData.account =
            requireNotNull(intent.getSerializable(ACCOUNT, Account::class.java)) {
                "Missing account extra"
            }
    }

    private fun goToAddTokens() {
        val intent = Intent(this, AddTokenActivity::class.java)
        intent.putExtra(ACCOUNT, viewModelTokens.tokenData.account)
        startActivity(intent)
    }
}