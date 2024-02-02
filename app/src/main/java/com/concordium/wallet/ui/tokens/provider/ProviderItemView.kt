package com.concordium.wallet.ui.tokens.provider

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.concordium.wallet.R

class ProviderItemView(context: Context, attrs: AttributeSet?, private val listener: IProviderItemView) : LinearLayout(context, attrs) {

    interface IProviderItemView {
        fun showProvider(provider: ProviderMeta)
        fun deleteProvider(provider: ProviderMeta)
    }

    private var provider: ProviderMeta? = null
    private var providerNameTv: TextView
    private var countTv: TextView
    private var arrow: ImageView

    init {
        inflate(context, R.layout.row_provider, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        providerNameTv = findViewById(R.id.provider_name)
        countTv = findViewById(R.id.count)
        arrow = findViewById(R.id.arrow)
    }

    fun setProvider(provider: ProviderMeta) {
        this.provider = provider
        providerNameTv.text = provider.name
        val count = provider.wallets.sumOf { it.total }
        countTv.text = "$count ${if (count == 1) "item" else "items"}"
        if (count > 0) {
            arrow.visibility = View.VISIBLE
        } else {
            arrow.visibility = View.GONE
        }
    }

    fun onClick() {
        provider?.let {
            listener.showProvider(it)
        }
    }
}