package com.concordium.wallet.ui.multinetwork

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class NetworkListItemAdapter(
    private val onNetworkItemClicked: (item: NetworkListItem.Network) -> Unit,
    private val onAddClicked: () -> Unit,
) : RecyclerView.Adapter<NetworkListItemAdapter.ViewHolder>() {

    private var data: List<NetworkListItem> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<NetworkListItem>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int =
        data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }
}
