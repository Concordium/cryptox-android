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
    private val showManageButton: Boolean
) : RecyclerView.Adapter<TokensAccountDetailsAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null
    private var addButtonClickListener: () -> Unit = {}
    private val iconSize: Int by lazy {
        context.resources.getDimensionPixelSize(R.dimen.cis_token_icon_size)
    }
    private val dataSet: MutableList<Token> = mutableListOf()
    private var dataSize = if (showManageButton) dataSet.size + 1 else dataSet.size

    inner class ViewHolder(val binding: ItemTokenAccountDetailsBinding) :
        RecyclerView.ViewHolder(binding.root)

    interface TokenClickListener {
        fun onRowClick(token: Token)
    }

    fun setTokenClickListener(tokenClickListener: TokenClickListener) {
        this.tokenClickListener = tokenClickListener
    }

    fun setManageButtonClickListener(clickListener: () -> Unit) {
        this.addButtonClickListener = clickListener
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Token>) {
        dataSet.clear()
        dataSet.addAll(data)
        dataSize = data.size
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (showManageButton) dataSize + 1 else dataSize

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

        holder.binding.eurRate.visibility = View.GONE

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
                .load(R.drawable.mw24_ic_ccd)
                .into(holder.binding.tokenIcon)

            holder.binding.eurRate.visibility = View.VISIBLE
            holder.binding.eurRate.text = if (showManageButton)
                context.getString(
                    R.string.cis_eur_rate,
                    CurrencyUtil.toEURRate(token.balance, token.denominator, token.numerator)
                )
            else
                context.getString(R.string.cis_at_disposal)

        } else {
            Glide.with(context)
                .load(R.drawable.ic_token_no_image)
                .into(holder.binding.tokenIcon)
        }

        if (token.isUnique) {
            holder.binding.apply {
                title.text = token.name
                balance.isVisible = false
                subtitle.isVisible = true
                subtitle.text =
                    if (token.balance > BigInteger.ZERO)
                        context.getString(R.string.cis_owned)
                    else
                        context.getString(R.string.cis_not_owned)
            }
        } else {
            holder.binding.title.text = token.symbol
            holder.binding.balance.isVisible = true
            holder.binding.balance.text = CurrencyUtil.formatAndRoundGTU(
                value = token.balance,
                roundDecimals = 2,
                decimals = token.decimals
            )
            holder.binding.subtitle.isVisible = false
        }
        holder.binding.apply {
            notice.isVisible = token.isNewlyReceived
            earningLabel.isVisible = token.isEarning
            content.setOnClickListener {
                tokenClickListener?.onRowClick(token)
            }
        }
    }
}
