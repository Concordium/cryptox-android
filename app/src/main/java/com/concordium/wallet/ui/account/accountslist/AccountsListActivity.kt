package com.concordium.wallet.ui.account.accountslist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityAccountsListBinding
import com.concordium.wallet.ui.account.accountsoverview.AccountsOverviewViewModel
import com.concordium.wallet.ui.account.newaccountname.NewAccountNameActivity
import com.concordium.wallet.ui.base.BaseActivity

class AccountsListActivity : BaseActivity(
    R.layout.activity_accounts_list,
    R.string.accounts_list_title
) {

    private val binding: ActivityAccountsListBinding by lazy {
        ActivityAccountsListBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var viewModel: AccountsOverviewViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        hideSettings(isVisible = true) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        }
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountsOverviewViewModel::class.java]

        binding.createAccountButton.setOnClickListener { gotoCreateAccount() }
    }

    private fun gotoCreateAccount() {
        val intent = Intent(this, NewAccountNameActivity::class.java)
        startActivity(intent)
    }


}