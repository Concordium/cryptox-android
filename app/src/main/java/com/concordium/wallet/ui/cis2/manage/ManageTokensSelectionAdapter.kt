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
import com.concordium.wallet.databinding.ItemTokenAddBinding
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import java.math.BigInteger

class ManageTokensSelectionAdapter(
    private val context: Context,
    var dataSet: Array<Token>,
) : RecyclerView.Adapter<ManageTokensSelectionAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null
    private val iconSize: Int by lazy {
        context.resources.getDimensionPixelSize(R.dimen.cis_token_icon_size)
    }

    inner class ViewHolder(val binding: ItemTokenAddBinding) : RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onRowClick(token: Token)
        fun onCheckBoxClick(token: Token)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }

    override fun getItemCount() = dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemTokenAddBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val token = dataSet[position]

        holder.binding.title.text = token.symbol
        holder.binding.subtitle.text = CurrencyUtil.formatCompactGTU(
            value = token.balance,
            decimals = token.decimals
        )

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

                if (metadata?.unique == true) {
                    holder.binding.subtitle.isVisible = true
                    holder.binding.subtitle.text =
                        if (token.balance > BigInteger.ZERO)
                            context.getString(R.string.cis_owned)
                        else
                            context.getString(R.string.cis_not_owned)
                }
            }

            is ProtocolLevelToken -> {
                Glide.with(context)
                    .load(R.drawable.mw24_ic_token_placeholder)
                    .into(holder.binding.tokenIcon)
            }
        }

        // Only allow selection when the metadata is loaded.
        holder.binding.selection.isVisible = !(token is ContractToken && token.metadata == null)
        holder.binding.selection.isChecked = token.isSelected

        holder.binding.root.setOnClickListener {
            tokenClickListener?.onRowClick(token)
        }
        holder.binding.selection.setOnClickListener {
            tokenClickListener?.onCheckBoxClick(token)
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
                    compoundDrawableTintList = ContextCompat.getColorStateList(
                        context,
                        R.color.mw24_plain_white_40
                    )
                }

                else -> isVisible = false
            }
        }
    }
}
