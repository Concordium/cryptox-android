package com.concordium.wallet.ui.cis2.manage

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ItemTokenAddBinding
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import java.math.BigInteger

class ManageTokensSelectionAdapter(
    private val context: Context,
    var dataSet: Array<NewToken>
) : RecyclerView.Adapter<ManageTokensSelectionAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null
    private val iconSize: Int by lazy {
        context.resources.getDimensionPixelSize(R.dimen.cis_token_icon_size)
    }

    inner class ViewHolder(val binding: ItemTokenAddBinding) : RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onRowClick(token: NewToken)
        fun onCheckBoxClick(token: NewToken)
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val token = dataSet[position]
        val tokenMetadata = token.metadata

        val thumbnailUrl = tokenMetadata?.thumbnail?.url?.takeUnless(String::isBlank)
        if (thumbnailUrl != null) {
            Glide.with(context)
                .load(thumbnailUrl)
                .placeholder(ThemedCircularProgressDrawable(context))
                .error(R.drawable.mw24_ic_token_placeholder)
                .override(iconSize)
                .fitCenter()
                .into(holder.binding.tokenIcon)
        } else if (tokenMetadata != null) {
            holder.binding.tokenIcon.setImageResource(R.drawable.mw24_ic_token_placeholder)
        } else {
            // While the metadata is loading, show progress in the icon view.
            holder.binding.tokenIcon.setImageDrawable(ThemedCircularProgressDrawable(context))
        }

        holder.binding.title.text = tokenMetadata?.name ?: if (token is PLTToken)
            token.tokenId
        else
            context.getString(R.string.cis_loading_metadata_progress)

        if (token.metadata?.unique == true) {
            holder.binding.subtitle.text =
                if (token.balance != BigInteger.ZERO)
                    context.getString(R.string.cis_owned)
                else
                    context.getString(R.string.cis_not_owned)
        } else {
            val tokenBalance = CurrencyUtil.formatGTU(
                token.balance,
                token.decimals
            )
            holder.binding.subtitle.text =
                context.getString(R.string.cis_search_balance, tokenBalance)
        }

        // Only allow selection when the metadata is loaded.
        holder.binding.selection.isVisible = token.metadata != null
        holder.binding.selection.isChecked = token.isSelected

        holder.binding.root.setOnClickListener {
            tokenClickListener?.onRowClick(token)
        }
        holder.binding.selection.setOnClickListener {
            tokenClickListener?.onCheckBoxClick(token)
        }
    }
}
