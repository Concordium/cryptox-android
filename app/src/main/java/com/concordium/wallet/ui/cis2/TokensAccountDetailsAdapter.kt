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
        when {
            dataSet.isEmpty() -> {
                holder.binding.manageTokens.visibility = View.GONE
                holder.binding.content.visibility = View.GONE
                return
            }

            (position == itemCount - 1 && showManageButton) -> {
                holder.binding.manageTokens.visibility = View.VISIBLE
                holder.binding.content.visibility = View.GONE
                return
            }

            else -> {
                holder.binding.manageTokens.visibility = View.GONE
                holder.binding.content.visibility = View.VISIBLE
            }
        }

        val token = dataSet[position]


        val tokenMetadata = token.metadata
        if (tokenMetadata?.thumbnail != null && !tokenMetadata.thumbnail.url.isNullOrBlank()) {
            Glide.with(context)
                .load(tokenMetadata.thumbnail.url)
                .override(iconSize)
                .placeholder(ThemedCircularProgressDrawable(context))
                .fitCenter()
                .into(holder.binding.tokenIcon)
        } else if (token.isCcd) {
            Glide.with(context)
                .load(R.drawable.cryptox_ico_ccd_light_40)
                .into(holder.binding.tokenIcon)
        } else {
            Glide.with(context)
                .load(R.drawable.ic_token_no_image)
                .into(holder.binding.tokenIcon)
        }

        if (token.isUnique) {
            holder.binding.title.text = token.name
            holder.binding.balance.isVisible = false
            holder.binding.subtitle.isVisible = true
            holder.binding.subtitle.text =
                if (token.balance > BigInteger.ZERO)
                    context.getString(R.string.cis_owned)
                else
                    context.getString(R.string.cis_not_owned)
        } else {
            holder.binding.title.text = token.symbol
            holder.binding.balance.text = CurrencyUtil.formatGTU(
                token.balance,
                token,
            )
            holder.binding.subtitle.isVisible = false
        }

        holder.binding.notice.isVisible = token.isNewlyReceived

        holder.binding.content.setOnClickListener {
            if (!token.isCcd || !showManageButton)
                tokenClickListener?.onRowClick(token)
        }
    }
}
