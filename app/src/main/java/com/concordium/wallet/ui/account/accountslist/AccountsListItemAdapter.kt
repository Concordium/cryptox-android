package com.concordium.wallet.ui.account.accountslist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ListItemAccountBinding
import com.concordium.wallet.ui.account.accountsoverview.AccountsOverviewListItem

class AccountsListItemAdapter(
    private val context: Context,
    private val data: List<AccountsOverviewListItem>
) : RecyclerView.Adapter<AccountsListItemAdapter.ViewHolder>() {

    class ViewHolder(private val context: Context, private val view: ListItemAccountBinding) :
        RecyclerView.ViewHolder(view.root) {
        fun bind(account: AccountWithIdentity) {
            view.accountName.text = if (account.account.isInitial())
                context.getString(
                    R.string.view_account_name_initial,
                    account.account.getAccountName()
                )
            else
                account.account.getAccountName()
            view.identityName.text = account.identity.name
            view.accountTotalBalance.text = CurrencyUtil.formatAndRoundGTU(
                value = account.account.balance,
                roundDecimals = 2
            )
            if (account.account.balance != account.account.balanceAtDisposal) {
                view.accountBalanceAtDisposal.visibility = View.VISIBLE
                view.accountBalanceAtDisposal.text = CurrencyUtil.formatAndRoundGTU(
                    value = account.account.balanceAtDisposal,
                    roundDecimals = 2
                )
            } else {
                view.accountBalanceAtDisposal.visibility = View.GONE
            }
            when {
                account.account.isBaking() -> {
                    view.earningPercent.visibility = View.VISIBLE
                    view.earningPercent.text = CurrencyUtil.formatAndRoundGTU(
                        value = account.account.stakedAmount,
                        roundDecimals = 2
                    )
                }
                account.account.isDelegating() -> {
                    view.earningPercent.visibility = View.VISIBLE
                    view.earningPercent.text = CurrencyUtil.formatAndRoundGTU(
                        value = account.account.delegatedAmount,
                        roundDecimals = 2
                    )
                }
                else -> view.earningPercent.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            context,
            ListItemAccountBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position] as AccountsOverviewListItem.Account
        holder.bind(item.accountWithIdentity)
    }

    override fun getItemCount(): Int = data.size
}