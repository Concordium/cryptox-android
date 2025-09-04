package com.concordium.wallet.ui.cis2.manage

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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

    @SuppressLint("UseCompatTextViewDrawableApis")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val token = tokensList[position]

        when (token) {
            is CCDToken -> {
                Glide.with(context)
                    .load(R.drawable.mw24_ic_ccd)
                    .into(holder.binding.tokenIcon)
            }

            is ContractToken -> {
                val metadata = token.metadata
                val iconUrl =
                    metadata
                        ?.thumbnail
                        ?.url
                        ?.takeIf(String::isNotBlank)

                Glide.with(context)
                    .load(iconUrl)
                    .override(iconSize)
                    .placeholder(ThemedCircularProgressDrawable(context))
                    .error(R.drawable.mw24_ic_token_placeholder)
                    .fitCenter()
                    .into(holder.binding.tokenIcon)
            }

            is ProtocolLevelToken -> {
                Glide.with(context)
                    .load(R.drawable.mw24_ic_token_placeholder)
                    .into(holder.binding.tokenIcon)
            }
        }

        holder.binding.title.text = token.symbol

        holder.binding.hideBtn.isVisible = token !is CCDToken
        holder.binding.hideBtn.setOnClickListener {
            tokenClickListener?.onHideClick(token)
        }

        holder.binding.pltInAllowListIcon.isVisible =
            token is ProtocolLevelToken && !token.isTransferable

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
                    compoundDrawableTintList = ContextCompat.getColorStateList(
                        context,
                        R.color.mw24_content_accent_secondary
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
                    compoundDrawableTintList =
                        ContextCompat.getColorStateList(context, R.color.mw24_plain_white_40)
                }

                else -> isVisible = false
            }
        }
        if (token is ContractToken && token.metadata?.unique == true) {
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
