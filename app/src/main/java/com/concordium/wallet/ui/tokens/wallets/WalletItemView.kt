package com.concordium.wallet.ui.tokens.wallets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.concordium.wallet.R
import com.concordium.wallet.data.model.WalletMeta

class WalletItemView(context: Context, attrs: AttributeSet?, private val listener: IWalletItemView) : LinearLayout(context, attrs), View.OnClickListener {

    interface IWalletItemView {
        fun onClick(wallet: WalletMeta)
    }

    private var wallet: WalletMeta? = null
    private var providerNameTv: TextView
    private var countTv: TextView

    init {
        inflate(context, R.layout.row_wallet, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        providerNameTv = findViewById(R.id.provider_name)
        countTv = findViewById(R.id.count)

        setOnClickListener(this)
    }

    fun setWallet(wallet: WalletMeta) {
        this.wallet = wallet
        providerNameTv.text = wallet.name.ifEmpty {
            wallet.address
        }
        countTv.text = "${wallet.total} ${if (wallet.total == 1) "item" else "items"}"
    }

    override fun onClick(p0: View?) {
        wallet?.let {
            listener.onClick(it)
        }
    }
}