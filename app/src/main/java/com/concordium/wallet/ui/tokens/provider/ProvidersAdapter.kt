package com.concordium.wallet.ui.tokens.provider

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.Constants
import com.concordium.wallet.R

class ProvidersAdapter(private val callback: ProviderItemView.IProviderItemView) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data: List<ProviderMeta> = emptyList()

    class ItemViewHolder(val view: ProviderItemView) : RecyclerView.ViewHolder(view) {

        fun bind(item: ProviderMeta) {
            view.setProvider(item)
        }
    }

    class MenuViewHolder(val view: MenuItemView) : RecyclerView.ViewHolder(view) {

        fun bind(item: ProviderMeta) {
            view.setProvider(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (data[position].isShowMenu) {
            Constants.Menu.SHOW
        } else {
            Constants.Menu.HIDE
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            Constants.Menu.SHOW -> {
                MenuViewHolder(MenuItemView(parent.context, null))
            }
            Constants.Menu.HIDE -> {
                ItemViewHolder(ProviderItemView(parent.context, null, callback))
            }
            else -> {
                ItemViewHolder(ProviderItemView(parent.context, null, callback))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
        when (holder) {
            is ItemViewHolder -> {
                holder.bind(item)
                if (!item.system) {
                    holder.view.setOnLongClickListener {
                        showMenu(position)
                        return@setOnLongClickListener true
                    }
                }

                holder.view.setOnClickListener {
                    closeMenu()
                    val count = item.wallets.sumOf { wall -> wall.total }
                    if (count > 0) {
                        (it as ProviderItemView).onClick()
                    }
                }
            }
            is MenuViewHolder -> {
                holder.bind(item)
                holder.view.setOnClickListener {
                    closeMenu()
                }
                holder.view.findViewById<TextView>(R.id.btnDelete).setOnClickListener {
                    callback.deleteProvider(item)
                }
            }
        }
    }

    fun setData(data: List<ProviderMeta>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun showMenu(position: Int) {
        data.map { it.isShowMenu = false }
        data[position].isShowMenu = true
        notifyItemChanged(position)
    }

    private fun closeMenu() {
        data.map { it.isShowMenu = false }
        notifyDataSetChanged()
    }
}