package com.concordium.wallet.ui.cis2

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ItemTokenAccountDetailsBinding
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import java.math.BigInteger

class TokensAccountDetailsAdapter(
    private val context: Context,
    private val showManageButton: Boolean,
    var dataSet: Array<Token>
) : RecyclerView.Adapter<TokensAccountDetailsAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null
    private var addButtonClickListener: () -> Unit = {}
    private val iconSize: Int by lazy {
        context.resources.getDimensionPixelSize(R.dimen.cis_token_icon_size)
    }

    inner class ViewHolder(val binding: ItemTokenAccountDetailsBinding) :
        RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onRowClick(token: Token)
        fun onCheckBoxClick(token: Token)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }

    fun setManageButtonClickListener(clickListener: () -> Unit) {
        this.addButtonClickListener = clickListener
    }

    override fun getItemCount() = if (showManageButton) dataSet.size + 1 else dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTokenAccountDetailsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.manageTokens.setOnClickListener {
            addButtonClickListener.invoke()
        }
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == itemCount - 1 && showManageButton) {
            holder.binding.manageTokens.visibility = View.VISIBLE
            holder.binding.content.visibility = View.GONE
            return
        } else {
            holder.binding.manageTokens.visibility = View.GONE
            holder.binding.content.visibility = View.VISIBLE
        }

        val token = dataSet[position]

        if (token.isCCDToken) {
            holder.binding.title.text =
                "${CurrencyUtil.formatGTU(token.totalBalance, true)} CCD"

            Glide.with(context)
                .load(R.drawable.cryptox_ico_ccd_light_40)
                .into(holder.binding.tokenIcon)
        } else {
            token.tokenMetadata?.let { tokenMetadata ->
                if (tokenMetadata.thumbnail != null && !tokenMetadata.thumbnail.url.isNullOrBlank()) {
                    Glide.with(context)
                        .load(tokenMetadata.thumbnail.url)
                        .override(iconSize)
                        .placeholder(ThemedCircularProgressDrawable(context))
                        .fitCenter()
                        .into(holder.binding.tokenIcon)
                } else {
                    holder.binding.tokenIcon.setImageResource(R.drawable.ic_token_no_image)
                }
                if (tokenMetadata.unique == true) {
                    holder.binding.title.text = tokenMetadata.name
                    holder.binding.subtitle.isVisible = true
                    holder.binding.subtitle.text =
                        if (token.totalBalance > BigInteger.ZERO)
                            context.getString(R.string.cis_owned)
                        else
                            context.getString(R.string.cis_not_owned)
                } else {
                    holder.binding.title.text = "${
                        CurrencyUtil.formatGTU(
                            token.totalBalance,
                            token,
                        )
                    } ${token.symbol}"
                    holder.binding.subtitle.isVisible = false
                }
            }
        }

        holder.binding.content.setOnClickListener {
            if (!token.isCCDToken || !showManageButton)
                tokenClickListener?.onRowClick(token)
        }
    }
}
