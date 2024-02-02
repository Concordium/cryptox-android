package com.concordium.wallet.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.uicore.view.IdentityView

class IdentityAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data: List<Identity> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    class ItemViewHolder(val view: IdentityView) : RecyclerView.ViewHolder(view) {
        fun bind(item: Identity, onItemClickListener: OnItemClickListener?) {
            view.setIdentityData(item)

            // Click
            view.setOnItemClickListener { identity ->
                onItemClickListener?.onItemClicked(identity)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_identity, parent, false) as IdentityView
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
        (holder as ItemViewHolder).bind(item, onItemClickListener)
    }

    fun setData(data: List<Identity>) {
        this.data = data
        notifyDataSetChanged()
    }

    //region OnItemClickListener
    // ************************************************************

    interface OnItemClickListener {
        fun onItemClicked(item: Identity)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}
