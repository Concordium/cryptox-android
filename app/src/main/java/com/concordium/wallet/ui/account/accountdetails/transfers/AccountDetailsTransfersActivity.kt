package com.concordium.wallet.ui.account.accountdetails.transfers

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityAccountDetailsTransfersBinding
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsErrorFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsPendingFragment
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable

class AccountDetailsTransfersActivity : BaseActivity(
    R.layout.activity_account_details_transfers,
    R.string.account_details_transfers_title
) {

    private val binding by lazy {
        ActivityAccountDetailsTransfersBinding.bind(findViewById(R.id.root_layout))
    }

    private lateinit var accountDetailsViewModel: AccountDetailsViewModel

    companion object {
        const val EXTRA_ACCOUNT = "EXTRA_ACCOUNT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)

        accountDetailsViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AccountDetailsViewModel::class.java]

        accountDetailsViewModel.account = intent.getSerializable(EXTRA_ACCOUNT, Account::class.java)

        replaceFragment(getCurrentFragment(accountDetailsViewModel.account))
    }

    private fun getCurrentFragment(account: Account): Fragment {
        return when (account.transactionStatus) {
            TransactionStatus.ABSENT -> AccountDetailsErrorFragment()
            TransactionStatus.COMMITTED -> AccountDetailsPendingFragment()
            TransactionStatus.RECEIVED -> AccountDetailsPendingFragment()
            else -> AccountDetailsTransfersFragment()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

}