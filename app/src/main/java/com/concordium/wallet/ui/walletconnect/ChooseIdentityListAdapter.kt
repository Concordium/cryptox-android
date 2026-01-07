package com.concordium.wallet.ui.walletconnect

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.view.isVisible
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.AccountInfoRowBinding

class ChooseIdentityListAdapter(
    private val context: Context,
    private var arrayList: List<Identity>,
) : BaseAdapter() {
    private var clickListener: ((Identity) -> Unit)? = null

    fun setOnClickListener(listener: (Identity) -> Unit) {
        this.clickListener = listener
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: AccountInfoRowBinding
        if (convertView == null) {
            binding = AccountInfoRowBinding.inflate(LayoutInflater.from(context), parent, false)
            binding.root.tag = binding
        } else {
            binding = convertView.tag as AccountInfoRowBinding
        }

        val identity = arrayList[position]

        with(binding) {
            accAddress.text = identity.name
            accBalance.isVisible = false
            accIdentity.isVisible = false
            accBalanceAtDisposal.isVisible = false

            root.setOnClickListener {
                clickListener?.invoke(identity)
            }
        }

        return binding.root
    }

    override fun getCount(): Int = arrayList.size

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }
}
