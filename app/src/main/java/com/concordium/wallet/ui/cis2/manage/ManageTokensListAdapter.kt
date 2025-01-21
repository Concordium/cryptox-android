package com.concordium.wallet.ui.cis2.manage

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.ItemTokenManageListBinding
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable

class ManageTokensListAdapter(
    private val context: Context,
) : RecyclerView.Adapter<ManageTokensListAdapter.ViewHolder>() {
    private val tokensList: MutableList<Token> = mutableListOf()
    private var tokenClickListener: TokenClickListener? = null
    private val iconSize: Int by lazy {
        context.resources.getDimensionPixelSize(R.dimen.cis_token_icon_size)
    }

    inner class ViewHolder(val binding: ItemTokenManageListBinding) :
        RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onHideClick(token: Token)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Token>) {
        tokensList.clear()
        tokensList.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemTokenManageListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = tokensList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val token = tokensList[position]
        val tokenMetadata = token.metadata

        val thumbnailUrl = tokenMetadata?.thumbnail?.url?.takeUnless(String::isBlank)
        if (thumbnailUrl != null) {
            Glide.with(context)
                .load(thumbnailUrl)
                .placeholder(ThemedCircularProgressDrawable(context))
                .override(iconSize)
                .fitCenter()
                .into(holder.binding.tokenIcon)
        } else if (token.isCcd) {
            Glide.with(context)
                .load(R.drawable.mw24_ic_ccd)
                .into(holder.binding.tokenIcon)
        } else if (tokenMetadata != null) {
            holder.binding.tokenIcon.setImageResource(R.drawable.ic_token_no_image)
        } else {
            // While the metadata is loading, show progress in the icon view.
            holder.binding.tokenIcon.setImageDrawable(ThemedCircularProgressDrawable(context))
        }

        holder.binding.title.text =
            tokenMetadata?.symbol ?: context.getString(R.string.cis_loading_metadata_progress)

        holder.binding.hideBtn.isVisible = !token.isCcd
        holder.binding.hideBtn.setOnClickListener {
            tokenClickListener?.onHideClick(token)
        }
    }


}