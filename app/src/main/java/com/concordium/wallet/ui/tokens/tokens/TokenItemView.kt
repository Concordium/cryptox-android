package com.concordium.wallet.ui.tokens.tokens

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.ui.tokens.provider.Token

class TokenItemView(
    context: Context,
    private val walletName: String?,
    attrs: AttributeSet?,
    private val listener: ITokenItemView
) : LinearLayout(context, attrs), View.OnClickListener {

    interface ITokenItemView {
        fun onClick(token: Token)
        fun showFoundCount(count: Int)
    }

    private var token: Token? = null
    private var walletNameTv: TextView
    private var tokenTv: TextView
    private var image: ImageView

    init {
        inflate(context, R.layout.row_token, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        walletNameTv = findViewById(R.id.wallet)
        tokenTv = findViewById(R.id.name)
        image = findViewById(R.id.token_image)

        setOnClickListener(this)
    }

    fun setToken(token: Token) {
        this.token = token
        Glide.with(this).load(token.iconPreviewUrl)
            .placeholder(R.color.cryptox_grey_additional_10)
            .override(400)
            .transform()
            .into(image)
        walletNameTv.text = walletName
        tokenTv.text = token.nftName
    }

    override fun onClick(p0: View?) {
        token?.let {
            listener.onClick(it)
        }
    }
}
