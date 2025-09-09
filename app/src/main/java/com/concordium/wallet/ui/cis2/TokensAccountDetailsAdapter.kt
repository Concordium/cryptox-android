package com.concordium.wallet.ui.cis2

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ItemTokenAccountDetailsBinding
import java.math.BigInteger

class TokensAccountDetailsAdapter(
    private val context: Context,
    private val showManageButton: Boolean,
) : RecyclerView.Adapter<TokensAccountDetailsAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null
    private var addButtonClickListener: () -> Unit = {}
    private val iconSize: Int by lazy {
        context.resources.getDimensionPixelSize(R.dimen.cis_token_icon_size)
    }
    private val dataSet: MutableList<Token> = mutableListOf()
    private var dataSize = if (showManageButton) dataSet.size + 1 else dataSet.size

    inner class ViewHolder(
        val binding: ItemTokenAccountDetailsBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        val iconView = TokenIconView(binding.tokenIcon)
    }

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

        with(holder.binding) {

            if (token is ContractToken && token.isUnique) {
                balance.isVisible = false
                subtitle.isVisible = true
                subtitle.text =
                    if (token.balance > BigInteger.ZERO)
                        context.getString(R.string.cis_owned)
                    else
                        context.getString(R.string.cis_not_owned)
            } else {
                balance.isVisible = true
                balance.text = CurrencyUtil.formatAndRoundGTU(
                    value = token.balance,
                    roundDecimals = 2,
                    decimals = token.decimals,
                )
                subtitle.isVisible = false
            }

            if (token is CCDToken) {
                eurRate.isVisible = true
                eurRate.text =
                    if (showManageButton) {
                        if (token.eurPerMicroCcd != null) {
                            context.getString(
                                R.string.cis_eur_rate,
                                CurrencyUtil.toEURRate(
                                    token.balance,
                                    token.eurPerMicroCcd
                                )
                            )
                        } else {
                            ""
                        }
                    } else {
                        context.getString(R.string.cis_at_disposal)
                    }
            } else {
                eurRate.isVisible = false
            }

            title.text =
                if (token is ContractToken && token.isUnique)
                    token.metadata?.name ?: ""
                else
                    token.symbol

            notice.isVisible = token.isNewlyReceived
            earningLabel.isVisible = token is CCDToken && token.isEarning
            pltInAllowListIcon.isVisible = token is ProtocolLevelToken && !token.isTransferable

            holder.iconView.showTokenIcon(token)

            content.setOnClickListener {
                tokenClickListener?.onRowClick(token)
            }
        }
    }
}
