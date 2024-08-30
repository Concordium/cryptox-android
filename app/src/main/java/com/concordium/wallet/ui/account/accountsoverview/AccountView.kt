package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
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

        context.withStyledAttributes(attrs, R.styleable.AccountView) {
            binding.buttonsLayout.isVisible =
                getBoolean(R.styleable.AccountView_showButtons, true)
        }
    }

    fun setAccount(accountWithIdentity: AccountWithIdentity) {
        this.accountWithIdentity = accountWithIdentity

        binding.totalTextview.text =
            CurrencyUtil.formatGTU(accountWithIdentity.account.balance, withGStroke = true)

        binding.balanceAtDisposalTextview.text = CurrencyUtil.formatGTU(
            accountWithIdentity.account.balanceAtDisposal,
            withGStroke = true
        )

        binding.accountNameArea.setData(accountWithIdentity)

        binding.onrampBtn.isEnabled =
            accountWithIdentity.account.transactionStatus == TransactionStatus.FINALIZED
        binding.sendFundsBtn.isEnabled = !accountWithIdentity.account.readOnly
                && accountWithIdentity.account.transactionStatus == TransactionStatus.FINALIZED
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        val account = accountWithIdentity?.account
        if (onItemClickListener != null && account != null) {
            binding.root.setOnClickListener {
                onItemClickListener.onCardClicked(account)
            }

            binding.onrampBtn.setOnClickListener {
                onItemClickListener.onOnrampClicked(account)
            }

            binding.sendFundsBtn.setOnClickListener {
                onItemClickListener.onSendClicked(account)
            }

            binding.addressBtn.setOnClickListener {
                onItemClickListener.onAddressClicked(account)
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
