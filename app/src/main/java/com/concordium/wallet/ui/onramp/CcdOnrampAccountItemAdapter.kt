package com.concordium.wallet.ui.onramp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ListItemCcdOnrampAccountBinding

class CcdOnrampAccountItemAdapter(
    val onItemClicked: (CcdOnrampAccountListItem) -> Unit,
): RecyclerView.Adapter<CcdOnrampAccountItemAdapter.ViewHolder>() {
    private var data: List<CcdOnrampAccountListItem> = listOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: List<CcdOnrampAccountListItem>) {
        this.data = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_ccd_onramp_account, parent, false)
        )

    override fun getItemCount(): Int =
        data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        with(holder.binding) {
            accountNameTextView.text = item.accountName
            identityNameTextView.text = item.identityName
            balanceTextView.text = CurrencyUtil.formatGTU(item.balance)
            divider.isVisible = item.isDividerVisible

            root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ListItemCcdOnrampAccountBinding.bind(itemView)
    }
}
