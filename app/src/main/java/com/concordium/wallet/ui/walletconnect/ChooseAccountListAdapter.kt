package com.concordium.wallet.ui.walletconnect

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.AccountInfoRowBinding

class ChooseAccountListAdapter(private val context: Context, var arrayList: List<Account>) : BaseAdapter() {
    private var chooseAccountClickListener: ChooseAccountClickListener? = null

    fun interface ChooseAccountClickListener {
        fun onClick(accountWithIdentity: Account)
    }

    fun setChooseAccountClickListener(chooseAccountClickListener: ChooseAccountClickListener) {
        this.chooseAccountClickListener = chooseAccountClickListener
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val binding: AccountInfoRowBinding

        if (convertView == null) {
            binding = AccountInfoRowBinding.inflate(LayoutInflater.from(context), parent, false)
            holder = ViewHolder(binding)
            holder.binding.root.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val account = arrayList[position]
        val name = account.name
        holder.binding.accAddress.text =
            if (name.isNotEmpty())
                context.getString(
                    R.string.acc_address_placeholder,
                    account.name,
                    account.address
                )
            else
                account.address
        val atDisposalBalance = account.balanceAtDisposal()
        holder.binding.accBalance.text = context.getString(
            R.string.acc_balance_placeholder,
            CurrencyUtil.formatGTU(atDisposalBalance, true)
        )

        holder.binding.root.setOnClickListener {
            chooseAccountClickListener?.onClick(account)
        }

        return holder.binding.root
    }

    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    private inner class ViewHolder(val binding: AccountInfoRowBinding)
}
