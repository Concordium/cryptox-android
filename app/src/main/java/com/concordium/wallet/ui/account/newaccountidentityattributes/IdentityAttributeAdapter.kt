package com.concordium.wallet.ui.account.newaccountidentityattributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.databinding.ItemIdentityAttributeCheckableBinding

class IdentityAttributeAdapter :
    RecyclerView.Adapter<IdentityAttributeAdapter.ItemViewHolder>() {

    private var data: List<SelectableIdentityAttribute> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null

    class ItemViewHolder(
        val view: ItemIdentityAttributeCheckableBinding
    ) : RecyclerView.ViewHolder(view.root) {
        fun bind(
            item: SelectableIdentityAttribute,
            isLast: Boolean,
            onItemClickListener: OnItemClickListener?
        ) {
            val attributeKeyValue = IdentityAttributeConverterUtil.convertAttributeValue(
                view.root.context,
                Pair(item.name, item.value)
            )

            view.attributeNameTextview.text = attributeKeyValue.first
            view.attributeValueTextview.text = attributeKeyValue.second
            // Set the listener before assigning default value to the checked state
            view.attributeCheckBox.setOnCheckedChangeListener(null)
            view.attributeCheckBox.isChecked = item.isSelected

            view.attributeCheckBox.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onItemClickListener?.onCheckedChanged(item)
            }

            view.divider.isVisible = !isLast
            // Click
            view.rootLayout.setOnClickListener {
                view.attributeCheckBox.isChecked = !view.attributeCheckBox.isChecked
                onItemClickListener?.onItemClicked(item)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemIdentityAttributeCheckableBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(
            data[position],
            position == itemCount - 1,
            onItemClickListener
        )
    }

    fun setData(data: List<SelectableIdentityAttribute>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun getCheckedAttributes(): List<SelectableIdentityAttribute> {
        return data.filter(SelectableIdentityAttribute::isSelected)
    }

    //region OnItemClickListener
    // ************************************************************

    interface OnItemClickListener {
        fun onItemClicked(item: SelectableIdentityAttribute)
        fun onCheckedChanged(item: SelectableIdentityAttribute)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}
