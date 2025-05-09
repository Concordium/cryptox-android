package com.concordium.wallet.ui.payandverify

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ViewDemoPayAndVerifyAccountBinding
import com.concordium.wallet.util.ImageUtil

class DemoPayAndVerifyAccountListItemAdapter(
    val onItemClicked: (item: DemoPayAndVerifyAccountListItem) -> Unit,
) : RecyclerView.Adapter<DemoPayAndVerifyAccountListItemAdapter.ViewHolder>() {

    private var data: List<DemoPayAndVerifyAccountListItem> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_demo_pay_and_verify_account, parent, false),
        )

    override fun getItemCount(): Int =
        data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        with(holder.binding) {

            root.setOnClickListener {
                onItemClicked(item)
            }
            root.setBackgroundResource(
                if (item.isSelected)
                    R.drawable.mw24_item_account_background_active
                else
                    R.drawable.mw24_container_primary_background
            )

            accountName.text = item.account.account.getAccountName()
            identityName.text = item.account.identity.name
            accountIcon.setImageDrawable(
                ImageUtil.getIconById(
                    context = root.context,
                    id = item.account.account.iconId,
                )
            )
            amountTextView.text = CurrencyUtil.formatGTU(
                value = item.account.balance,
                decimals = item.account.tokenDecimals,
            )
            tokenSymbolTextView.text = item.account.tokenSymbol
            notValidBadge.isVisible = !item.isValid
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: List<DemoPayAndVerifyAccountListItem>) {
        this.data = items
        notifyDataSetChanged()
    }

    class ViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val binding = ViewDemoPayAndVerifyAccountBinding.bind(itemView)
    }
}
