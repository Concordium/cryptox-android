package com.concordium.wallet.ui.cis2.manage

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ItemTokenManageListBinding
import com.concordium.wallet.ui.cis2.TokenIconView
import com.concordium.wallet.ui.cis2.TokenTypeView

class ManageTokensListAdapter : RecyclerView.Adapter<ManageTokensListAdapter.ViewHolder>() {
    private val tokensList: MutableList<Token> = mutableListOf()
    private var tokenClickListener: TokenClickListener? = null

    inner class ViewHolder(
        val binding: ItemTokenManageListBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        val iconView = TokenIconView(binding.tokenIcon)
        val typeView = TokenTypeView(binding.tokenType)
    }

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

        holder.iconView.showTokenIcon(token)

        holder.binding.title.text =
            if (token is ContractToken && token.isUnique)
                token.metadata?.name ?: ""
            else
                token.symbol

        holder.binding.hideBtn.isVisible = token !is CCDToken
        holder.binding.hideBtn.setOnClickListener {
            tokenClickListener?.onHideClick(token)
        }

        holder.binding.pltInAllowListIcon.isVisible =
            token is ProtocolLevelToken && !token.isTransferable

        holder.typeView.showTokenType(token)

        if (token is ContractToken && token.isUnique) {
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
