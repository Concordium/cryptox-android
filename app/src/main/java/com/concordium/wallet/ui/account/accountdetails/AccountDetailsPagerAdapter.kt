package com.concordium.wallet.ui.account.accountdetails

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.accountdetails.identityattributes.AccountDetailsIdentityFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsErrorFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsPendingFragment
import com.concordium.wallet.ui.account.accountdetails.transfers.AccountDetailsTransfersFragment
import com.concordium.wallet.ui.cis2.TokensFragment

class AccountDetailsPagerAdapter(
    fragmentManager: FragmentManager,
    val account: Account,
    val context: Context
) :
    FragmentPagerAdapter(
        fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> getFirstPositionFragment()
            1 -> getSecondPositionFragment()
            else -> getThirdPositionFragment()
        }
    }

    override fun getCount(): Int {
        return 3
    }

    private fun getFirstPositionFragment(): Fragment {
        return when (account.transactionStatus) {
            TransactionStatus.ABSENT -> AccountDetailsErrorFragment()
            TransactionStatus.COMMITTED -> AccountDetailsPendingFragment()
            TransactionStatus.RECEIVED -> AccountDetailsPendingFragment()
            else -> AccountDetailsTransfersFragment()
        }
    }

    private fun getSecondPositionFragment(): Fragment {
        return when (account.transactionStatus) {
            TransactionStatus.ABSENT -> AccountDetailsErrorFragment()
            TransactionStatus.COMMITTED -> AccountDetailsPendingFragment()
            TransactionStatus.RECEIVED -> AccountDetailsPendingFragment()
            else -> TokensFragment()
        }
    }

    private fun getThirdPositionFragment() : Fragment {
        return when (account.transactionStatus) {
            TransactionStatus.ABSENT -> AccountDetailsErrorFragment()
            TransactionStatus.COMMITTED -> AccountDetailsPendingFragment()
            TransactionStatus.RECEIVED -> AccountDetailsPendingFragment()
            else -> {
                val fragment = AccountDetailsIdentityFragment()
                val bundle = Bundle()
                bundle.putSerializable(AccountDetailsIdentityFragment.EXTRA_ACCOUNT, account)
                fragment.arguments = bundle
                fragment
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> context.getString(R.string.account_details_transfers_title)
            1 -> context.getString(R.string.cis_tab_tokens)
            else -> context.getString(R.string.account_details_identity_data_title)
        }
    }
}
