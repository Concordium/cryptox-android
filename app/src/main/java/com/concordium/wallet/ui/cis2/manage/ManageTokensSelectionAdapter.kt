package com.concordium.wallet.ui.cis2.manage

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ItemTokenAddBinding
import com.concordium.wallet.ui.cis2.TokenIconView
import com.concordium.wallet.ui.cis2.TokenTypeView
import java.math.BigInteger

class ManageTokensSelectionAdapter(
    private val context: Context,
    var dataSet: Array<Token>,
) : RecyclerView.Adapter<ManageTokensSelectionAdapter.ViewHolder>() {
    private var tokenClickListener: TokenClickListener? = null

    inner class ViewHolder(
        val binding: ItemTokenAddBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        val iconView = TokenIconView(binding.tokenIcon)
        val typeView = TokenTypeView(binding.tokenType)
    }

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

        holder.binding.title.text =
            if (token is ContractToken && token.isUnique)
                token.metadata?.name ?: ""
            else
                token.symbol

        if (token is ContractToken && token.isUnique) {
            holder.binding.subtitle.text =
                if (token.balance > BigInteger.ZERO)
                    context.getString(R.string.cis_owned)
                else
                    context.getString(R.string.cis_not_owned)
        } else {
            holder.binding.subtitle.text = CurrencyUtil.formatCompactGTU(
                value = token.balance,
                decimals = token.decimals
            )
        }

        holder.iconView.showTokenIcon(token)

        // Only allow selection when the metadata is loaded.
        holder.binding.selection.isVisible = !(token is ContractToken && token.metadata == null)
        holder.binding.selection.isChecked = token.isSelected

        holder.binding.root.setOnClickListener {
            tokenClickListener?.onRowClick(token)
        }
        holder.binding.selection.setOnClickListener {
            tokenClickListener?.onCheckBoxClick(token)
        }

        holder.binding.pltInAllowListIcon.isVisible =
            token is ProtocolLevelToken && !token.isTransferable

        holder.typeView.showTokenType(token)
    }
}
