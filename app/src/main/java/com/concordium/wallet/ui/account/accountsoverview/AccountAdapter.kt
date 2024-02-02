package com.concordium.wallet.ui.account.accountsoverview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.room.AccountWithIdentity

class AccountAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var data: List<AccountWithIdentity> = emptyList()
    private var onItemClickListener: AccountView.OnItemClickListener? = null

    class ItemViewHolder(val view: AccountView) : RecyclerView.ViewHolder(view) {
        fun bind(item: AccountWithIdentity, onItemClickListener: AccountView.OnItemClickListener?) {
            view.setAccount(item)
            view.setOnItemClickListener(onItemClickListener)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_account, parent, false) as AccountView
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data.get(position)
        (holder as ItemViewHolder).bind(item, onItemClickListener)
    }

    fun setData(data: List<AccountWithIdentity>) {
        this.data = data
        notifyDataSetChanged()
    }

    //region OnItemClickListener
    // ************************************************************

    fun setOnItemClickListener(onItemClickListener: AccountView.OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}
