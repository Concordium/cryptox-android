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
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.util.CurrencyUtil
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
            if (token is ProtocolLevelToken) token.tokenId
            else tokenMetadata?.symbol ?: tokenMetadata?.name

        holder.binding.hideBtn.isVisible = token !is CCDToken
        holder.binding.hideBtn.setOnClickListener {
            tokenClickListener?.onHideClick(token)
        }

        holder.binding.pltInAllowListIcon.isVisible = if (token is ProtocolLevelToken) {
            token.isTransferable.not()
        } else {
            false
        }
        holder.binding.tokenType.apply {
            when (token) {
                is ProtocolLevelToken -> {
                    text = context.getString(R.string.token_type_plt)
                    setTextColor(context.getColor(R.color.mw24_content_accent_secondary))
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.mw24_ic_plt_token,
                        0,
                        0,
                        0
                    )
                }

                is ContractToken -> {
                    text = context.getString(R.string.token_type_cis)
                    setTextColor(context.getColor(R.color.mw24_plain_white_40))
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.mw24_ic_cis2_token,
                        0,
                        0,
                        0
                    )
                }

                else -> {
                    isVisible = false
                }
            }
        }
        if (token.metadata?.unique == true) {
            holder.binding.balance.isVisible = false
        } else {
            holder.binding.balance.isVisible = true
            holder.binding.balance.text = CurrencyUtil.formatCompactGTU(
                value = token.balance,
                decimals = token.decimals
            )
        }
    }
}