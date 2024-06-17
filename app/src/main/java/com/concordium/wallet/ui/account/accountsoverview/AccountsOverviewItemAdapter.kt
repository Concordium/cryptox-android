package com.concordium.wallet.ui.account.accountsoverview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.room.AccountWithIdentity

class AccountsOverviewItemAdapter(
    private val accountViewClickListener: AccountView.OnItemClickListener,
    private val onCcdOnrampBannerClicked: () -> Unit,
) : RecyclerView.Adapter<AccountsOverviewItemAdapter.ViewHolder>() {
    private var data: List<AccountsOverviewListItem> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<AccountsOverviewListItem>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int =
        data.size

    override fun getItemViewType(position: Int): Int = when (data[position]) {
        is AccountsOverviewListItem.Account ->
            R.layout.list_item_accounts_overview_account

        AccountsOverviewListItem.CcdOnrampBanner ->
            R.layout.list_item_accounts_overview_ccd_onramp_banner
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.list_item_accounts_overview_account ->
                ViewHolder.Account(view)

            R.layout.list_item_accounts_overview_ccd_onramp_banner ->
                ViewHolder.CcdOnrampBanner(view)

            else ->
                error("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder.Account -> {
                val item = data[position] as AccountsOverviewListItem.Account
                holder.accountView.setAccount(item.accountWithIdentity)
                holder.accountView.setOnItemClickListener(accountViewClickListener)
            }

            is ViewHolder.CcdOnrampBanner -> {
                holder.itemView.setOnClickListener {
                    onCcdOnrampBannerClicked()
                }
            }
        }
    }

    sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class CcdOnrampBanner(itemView: View) : ViewHolder(itemView)

        class Account(itemView: View) : ViewHolder(itemView) {
            val accountView = itemView as AccountView
        }
    }

    class ItemViewHolder(val view: AccountView) : RecyclerView.ViewHolder(view) {
        fun bind(item: AccountWithIdentity, onItemClickListener: AccountView.OnItemClickListener?) {
            view.setAccount(item)
            view.setOnItemClickListener(onItemClickListener)
        }
    }
}
