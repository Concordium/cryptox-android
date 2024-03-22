package com.concordium.wallet.ui.account.accountdetails.identityattributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.IdentityAttribute
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.databinding.ItemAccountIdentityAttributeBinding

class IdentityAttributeAdapter(
    private var data: List<IdentityAttribute>,
    private val providerName: String
) :
    RecyclerView.Adapter<IdentityAttributeAdapter.ItemViewHolder>() {

    class ItemViewHolder(
        private val view: ItemAccountIdentityAttributeBinding
    ) : RecyclerView.ViewHolder(view.root) {

        fun bind(
            item: IdentityAttribute,
            providerName: String
        ) {
            val attributeKeyValue = IdentityAttributeConverterUtil.convertAttribute(
                view.root.context,
                Pair(item.name, item.value)
            )
            view.attributeNameTextview.text = attributeKeyValue.first
            view.attributeValueTextview.text = attributeKeyValue.second
            view.providerNameTextview.text = providerName
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemAccountIdentityAttributeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(data[position], providerName)
    }
}
