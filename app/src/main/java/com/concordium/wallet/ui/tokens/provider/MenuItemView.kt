package com.concordium.wallet.ui.tokens.provider

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.concordium.wallet.R

class MenuItemView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs), View.OnClickListener {

    private var provider: ProviderMeta? = null
    private var providerNameTv: TextView
    private var countTv: TextView

    init {
        inflate(context, R.layout.row_provider_menu, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        providerNameTv = findViewById(R.id.provider_name)
        countTv = findViewById(R.id.count)
    }

    fun setProvider(provider: ProviderMeta) {
        this.provider = provider
        providerNameTv.text = provider.name
        val count = provider.wallets.sumOf { it.total }
        countTv.text = "$count ${if (count == 1) "item" else "items"}"
    }

    override fun onClick(p0: View?) {
        println()
    }
}