package com.concordium.wallet.ui.identity.identityproviderlist

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.databinding.ItemIdentityProviderBinding
import com.concordium.wallet.util.ImageUtil

class IdentityProviderAdapter(
    private var data: List<IdentityProvider>,
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

        val logoBitmap: Bitmap? =
            identityProvider
                .metadata
                .icon
                .takeIf(String::isNotEmpty)
                ?.let(ImageUtil::getImageBitmap)

        Glide.with(binding.logoImageview.context)
            .load(logoBitmap)
            .placeholder(R.drawable.circle_bg)
            .circleCrop()
            .into(binding.logoImageview)

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
