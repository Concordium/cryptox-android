package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.ManageTokensViewModel
import com.concordium.wallet.ui.cis2.manage.ManageTokenListActivity.Companion.ACCOUNT
import com.concordium.wallet.util.getSerializable

class AddTokenActivity: BaseActivity(
    R.layout.activity_add_token,
    R.string.cis_add_token_title
) {

    lateinit var viewModelTokens: ManageTokensViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)

        viewModelTokens = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[ManageTokensViewModel::class.java]

        viewModelTokens.tokenData.account =
            requireNotNull(intent.getSerializable(ACCOUNT, Account::class.java)) {
                "Missing account extra"
            }
    }
}