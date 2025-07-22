package com.concordium.wallet.ui.cis2.manage

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.databinding.ItemTokenManageListBinding
import com.concordium.wallet.ui.plt.PLTListStatus
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import com.concordium.wallet.util.TokenUtil

class ManageTokensListAdapter(
    private val context: Context,
) : RecyclerView.Adapter<ManageTokensListAdapter.ViewHolder>() {
    private val tokensList: MutableList<NewToken> = mutableListOf()
    private var tokenClickListener: TokenClickListener? = null
    private val iconSize: Int by lazy {
        context.resources.getDimensionPixelSize(R.dimen.cis_token_icon_size)
    }

    inner class ViewHolder(val binding: ItemTokenManageListBinding) :
        RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onHideClick(token: NewToken)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<NewToken>) {
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
                .error(R.drawable.mw24_ic_token_placeholder)
                .into(holder.binding.tokenIcon)
        } else if (token is CCDToken) {
            Glide.with(context)
                .load(R.drawable.mw24_ic_ccd)
                .into(holder.binding.tokenIcon)
        } else if (tokenMetadata != null) {
            holder.binding.tokenIcon.setImageResource(R.drawable.mw24_ic_token_placeholder)
        } else {
            Glide.with(context)
                .load(R.drawable.mw24_ic_token_placeholder)
                .into(holder.binding.tokenIcon)
        }

        holder.binding.title.text =
            if (token is PLTToken) token.tokenId
            else tokenMetadata?.symbol ?: tokenMetadata?.name

        holder.binding.hideBtn.isVisible = token !is CCDToken
        holder.binding.hideBtn.setOnClickListener {
            tokenClickListener?.onHideClick(token)
        }

        holder.binding.pltInAllowListIcon.isVisible = if (token is PLTToken) {
            TokenUtil.getPLTPLTListStatus(token) == PLTListStatus.NOT_ON_ALLOW_LIST ||
                    TokenUtil.getPLTPLTListStatus(token) == PLTListStatus.ON_DENY_LIST
        } else {
            false
        }
    }
}