package com.concordium.wallet.ui.account.accountdetails.transfers

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.databinding.ItemFooterProgressBinding
import com.concordium.wallet.databinding.ItemTransactionBinding
import com.concordium.wallet.ui.common.TransactionViewHelper
import com.concordium.wallet.uicore.recyclerview.BaseAdapter
import com.concordium.wallet.uicore.recyclerview.pinnedheader.PinnedHeaderListener

@SuppressLint("NotifyDataSetChanged")
class TransactionAdapter :
    BaseAdapter<AdapterItem>(mutableListOf()),
    PinnedHeaderListener {

    private var onItemClickListener: OnItemClickListener? = null

    inner class ItemViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding =
            ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as ItemViewHolder) {
            val transactionItem = items[position] as TransactionItem
            val transaction = transactionItem.transaction as Transaction
            TransactionViewHelper.show(
                transaction,
                binding.titleTextview,
                binding.subheaderTextview,
                binding.totalTextview,
                binding.costTextview,
                binding.layoutMemo,
                binding.memoTextview,
//                binding.amountTextview,
                binding.alertImageview,
                binding.statusImageview,
            )

            if (onItemClickListener != null) {
                binding.itemRootLayout.setOnClickListener {
                    onItemClickListener?.onItemClicked(transaction)
                }
            }
        }
    }

    fun setData(data: List<AdapterItem>) {
        clear()
        addAll(data)
        notifyDataSetChanged()
    }

    fun onDataSetChanged() {
        notifyDataSetChanged()
    }

    override fun createDummyItemForFooter(): AdapterItem {
        return TransactionItem()
    }

    override fun getItemViewType(position: Int): Int {
        if (items[position].getItemType() == AdapterItem.ItemType.Header) {
            return HEADER
        }
        return super.getItemViewType(position)
    }

    interface OnItemClickListener {
        fun onItemClicked(item: Transaction)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var itemPos = itemPosition
        var headerPosition = 0
        do {
            if (this.isHeader(itemPos)) {
                headerPosition = itemPos
                break
            }
            itemPos -= 1
        } while (itemPos >= 0)
        return headerPosition
    }

    override fun getHeaderLayout(headerPosition: Int): Int {
        return R.layout.item_header
    }

    override fun bindHeaderData(headerView: View, headerPosition: Int) {
        when (val item = items[headerPosition]) {
            is HeaderItem -> {
                (headerView as TextView).text = item.title
            }
        }
    }

    override fun isHeader(itemPosition: Int): Boolean {
        val item = items[itemPosition]
        return (item.getItemType() == AdapterItem.ItemType.Header)
    }

    // Header
    inner class HeaderViewHolder(val view: TextView) : RecyclerView.ViewHolder(view)

    override fun onCreateHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return HeaderViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_header, parent, false) as TextView
        )
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        with(holder as HeaderViewHolder) {
            val item = items[position] as HeaderItem
            view.text = item.title
        }
    }

    // Footer
    inner class FooterViewHolder(val binding: ItemFooterProgressBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateFooterViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding =
            ItemFooterProgressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FooterViewHolder(binding)
    }

    override fun onBindFooterViewHolder(holder: RecyclerView.ViewHolder) {
    }
}
