package com.concordium.wallet.ui.more.unshielding

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.databinding.ListItemUnshieldingAccountBinding

class UnshieldingAccountItemAdapter(
    val onUnshieldClicked: (item: UnshieldingAccountListItem) -> Unit,
) :
    RecyclerView.Adapter<UnshieldingAccountItemAdapter.ViewHolder>() {
    private var data: List<UnshieldingAccountListItem> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ListItemUnshieldingAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).let(::ViewHolder)

    override fun getItemCount(): Int =
        data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        with(holder.binding) {
            accountNameTextView.text = item.name

            unshieldButton.isVisible = !item.isUnshielded
            unshieldButton.setOnClickListener {
                onUnshieldClicked(item)
            }

            unshieldedTextView.isVisible = item.isUnshielded

            balanceTextView.text = item.balance ?: "*** ***"
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: List<UnshieldingAccountListItem>) {
        this.data = items
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ListItemUnshieldingAccountBinding) :
        RecyclerView.ViewHolder(binding.root)
}
