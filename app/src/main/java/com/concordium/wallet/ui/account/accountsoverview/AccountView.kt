package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.concordium.wallet.R
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ViewAccountBinding

class AccountView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private var accountWithIdentity: AccountWithIdentity? = null
    private val binding: ViewAccountBinding

    init {
        LayoutInflater.from(context).inflate(R.layout.view_account, this, true)
        binding = ViewAccountBinding.bind(this)

    }

    fun setAccount(accountWithIdentity: AccountWithIdentity) {
        this.accountWithIdentity = accountWithIdentity

        binding.totalTextview.text =
            CurrencyUtil.formatAndRoundGTU(accountWithIdentity.account.balance, roundDecimals = 2)

        if (accountWithIdentity.account.balanceAtDisposal != accountWithIdentity.account.balance) {
            binding.balanceAtDisposalTextview.visibility = View.VISIBLE
            binding.balanceAtDisposalTextview.text = CurrencyUtil.formatAndRoundGTU(
                accountWithIdentity.account.balanceAtDisposal,
                roundDecimals = 2
            )
        } else {
            binding.balanceAtDisposalTextview.visibility = View.GONE
        }

        binding.accountNameArea.setData(accountWithIdentity)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        val account = accountWithIdentity?.account
        if (onItemClickListener != null && account != null) {
            binding.root.setOnClickListener {
                onItemClickListener.onCardClicked(account)
            }
        }
    }

    interface OnItemClickListener {
        fun onCardClicked(account: Account)
        fun onOnrampClicked(account: Account)
        fun onSendClicked(account: Account)
        fun onAddressClicked(account: Account)
    }
}
