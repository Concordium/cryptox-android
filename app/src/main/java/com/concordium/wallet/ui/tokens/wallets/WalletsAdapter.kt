package com.concordium.wallet.ui.tokens.wallets

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.WalletMeta

class WalletsAdapter(private val callback: WalletItemView.IWalletItemView) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data: List<WalletMeta> = emptyList()

    class ItemViewHolder(val view: WalletItemView) : RecyclerView.ViewHolder(view) {

        fun bind(item: WalletMeta) {
            view.setWallet(item)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            WalletItemView(parent.context, null, callback)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
        (holder as ItemViewHolder).bind(item)
    }

    fun setData(data: List<WalletMeta>) {
        this.data = data
        notifyDataSetChanged()
    }
}