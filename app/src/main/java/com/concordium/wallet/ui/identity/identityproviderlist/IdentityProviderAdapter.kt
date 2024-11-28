package com.concordium.wallet.ui.identity.identityproviderlist

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.databinding.ItemIdentityProviderBinding
import com.concordium.wallet.util.ImageUtil.getImageBitmap

class IdentityProviderAdapter(
    private var data: List<IdentityProvider>
) : RecyclerView.Adapter<IdentityProviderAdapter.ItemViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    inner class ItemViewHolder(val binding: ItemIdentityProviderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ItemViewHolder(
            ItemIdentityProviderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) = with(holder) {
        val identityProvider = data[position]
        if (!TextUtils.isEmpty(identityProvider.metadata.icon)) {
            val image = getImageBitmap(identityProvider.metadata.icon)
            binding.logoImageview.setImageBitmap(image)
        }
        binding.headerTextview.text = identityProvider.displayName
        binding.itemDivider.isVisible = position != data.size - 1

        // Click
        if (onItemClickListener != null) {
            binding.rootLayout.setOnClickListener {
                onItemClickListener?.onItemClicked(identityProvider)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<IdentityProvider>) {
        this.data = data
        notifyDataSetChanged()
    }

    //region OnItemClickListener
    //************************************************************

    fun interface OnItemClickListener {
        fun onItemClicked(item: IdentityProvider)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //endregion
}
