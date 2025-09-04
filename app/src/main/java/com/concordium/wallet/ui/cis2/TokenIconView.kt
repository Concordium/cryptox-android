package com.concordium.wallet.ui.cis2

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.WithThumbnail
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable

class TokenIconView(
    private val iconImageView: ImageView,
) {
    private val context: Context
        get() = iconImageView.context

    private val iconSize: Int =
        context.resources.getDimensionPixelSize(R.dimen.cis_token_icon_size)

    fun showTokenIcon(token: Token) = when (token) {
        is CCDToken -> {
            Glide.with(context)
                .load(R.drawable.mw24_ic_ccd)
                .into(iconImageView)
        }

        is WithThumbnail -> {
            Glide.with(context)
                .load(token.thumbnailUrl)
                .override(iconSize)
                .placeholder(ThemedCircularProgressDrawable(context))
                .error(R.drawable.mw24_ic_token_placeholder)
                .fitCenter()
                .into(iconImageView)
        }

        else -> {
            Glide.with(context)
                .load(R.drawable.mw24_ic_token_placeholder)
                .into(iconImageView)
        }
    }
}
