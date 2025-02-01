package com.concordium.wallet.ui.account.accountslist

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityAccountsListBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.account.accountdetails.AccountSettingsActivity
import com.concordium.wallet.ui.account.newaccountname.NewAccountNameActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.multiwallet.FileWalletCreationLimitationDialog

class AccountsListActivity : BaseActivity(
    R.layout.activity_accounts_list,
    R.string.accounts_list_title
) {

    private val binding: ActivityAccountsListBinding by lazy {
        ActivityAccountsListBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var viewModelAccountDetails: AccountDetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
        viewModelAccountDetails = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountDetailsViewModel::class.java]

        viewModelAccountDetails.updateAccount()
        viewModelAccountDetails.newAccount.collectWhenStarted(this) { account ->
            hideSettings(isVisible = true) {
                gotoAccountSettings(account)
            }
        }
        binding.createAccountButton.setOnClickListener {
            if (viewModelAccountDetails.isCreationLimitedForFileWallet) {
                showFileWalletCreationLimitation()
            } else {
                gotoCreateAccount()
            }
        }
    }

    private fun showFileWalletCreationLimitation() {
        FileWalletCreationLimitationDialog().showSingle(
            supportFragmentManager,
            FileWalletCreationLimitationDialog.TAG
        )
    }

    private fun gotoCreateAccount() {
        val intent = Intent(this, NewAccountNameActivity::class.java)
        startActivity(intent)
    }

    private fun gotoAccountSettings(account: Account) {
        startActivity(Intent(this, AccountSettingsActivity::class.java).apply {
            putExtra(AccountSettingsActivity.EXTRA_ACCOUNT, account)
        })
    }

}