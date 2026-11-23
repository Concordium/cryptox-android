package com.concordium.wallet.ui.cis2

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.WithThumbnail
import com.concordium.wallet.uicore.view.ThemedCircularProgressDrawable
import com.concordium.wallet.util.IpfsGatewayFallback

class TokenIconView(
    private val iconImageView: ImageView,
) {
    private val context: Context
        get() = iconImageView.context

    private val iconSize: Int =
        context.resources.getDimensionPixelSize(
            R.dimen.cis_token_icon_size
        )

    private var lastThumbnailUrl: String? = null
    private var thumbnailInitialized = false

    fun showTokenIcon(token: Token) {
        when (token) {
            is CCDToken -> {
                lastThumbnailUrl = null
                thumbnailInitialized = false

                Glide.with(context)
                    .load(R.drawable.mw24_ic_ccd)
                    .into(iconImageView)
            }

            is WithThumbnail -> {
                val originalUrl = token.thumbnailUrl

                if (!thumbnailInitialized || originalUrl != lastThumbnailUrl) {
                    lastThumbnailUrl = originalUrl
                    thumbnailInitialized = true

                    val urls = if (originalUrl != null) {
                        IpfsGatewayFallback.getUrls(originalUrl)
                    } else {
                        emptyList()
                    }

                    buildImageRequest(urls)
                        .placeholder(ThemedCircularProgressDrawable(context))
                        .error(R.drawable.mw24_ic_token_placeholder)
                        .into(iconImageView)
                }
            }

            else -> {
                lastThumbnailUrl = null
                thumbnailInitialized = false

                Glide.with(context)
                    .load(R.drawable.mw24_ic_token_placeholder)
                    .into(iconImageView)
            }
        }
    }

    private fun buildImageRequest(
        urls: List<String>,
    ): RequestBuilder<Drawable> {

        if (urls.isEmpty()) {
            return Glide.with(context)
                .load(R.drawable.mw24_ic_token_placeholder)
        }

        // First try cached images from all available gateways.
        var networkRequest: RequestBuilder<Drawable>? = null

        for (url in urls.asReversed()) {
            val nextRequest = Glide.with(context)
                .asDrawable()
                .load(url)
                .override(iconSize)
                .fitCenter()

            if (networkRequest != null) {
                nextRequest.error(networkRequest)
            }

            networkRequest = nextRequest
        }

        // Cache-only requests are attempted before network requests.
        var request = requireNotNull(networkRequest)

        for (url in urls.asReversed()) {
            request = Glide.with(context)
                .asDrawable()
                .load(url)
                .override(iconSize)
                .fitCenter()
                .onlyRetrieveFromCache(true)
                .error(request)
        }

        return request
    }
}
