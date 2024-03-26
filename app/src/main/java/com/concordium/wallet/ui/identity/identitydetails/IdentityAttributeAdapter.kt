package com.concordium.wallet.ui.identity.identitydetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.databinding.ItemIdentityAttributeBinding
import java.util.SortedMap

class IdentityAttributeAdapter(private var data: SortedMap<String, String>) :
    RecyclerView.Adapter<IdentityAttributeAdapter.ItemViewHolder>() {

    var keys = ArrayList(data.keys)

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemIdentityAttributeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            val name = keys[position]
            data[name]?.let { value ->
                val attributeKeyValue = IdentityAttributeConverterUtil.convertAttribute(
                    binding.root.context,
                    Pair(name, value)
                )
                binding.attributeNameTextview.text = attributeKeyValue.first
                binding.attributeValueTextview.text = attributeKeyValue.second
                binding.divider.isVisible = position != itemCount - 1
            }
        }
    }

    inner class ItemViewHolder(val binding: ItemIdentityAttributeBinding) :
        RecyclerView.ViewHolder(binding.root)
}
