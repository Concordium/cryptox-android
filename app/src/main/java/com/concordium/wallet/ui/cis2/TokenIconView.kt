package com.concordium.wallet.ui.cis2

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.Target
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

    private val loadingPlaceholder by lazy {
        ThemedCircularProgressDrawable(context)
    }

    private var lastThumbnailUrl: String? = null
    private var thumbnailTarget: Target<Drawable>? = null
    private var thumbnailRequest: Request? = null

    private fun resetThumbnailState() {
        lastThumbnailUrl = null
        thumbnailTarget = null
        thumbnailRequest = null
    }

    fun showTokenIcon(token: Token) {
        when (token) {
            is CCDToken -> {
                resetThumbnailState()

                Glide.with(context)
                    .load(R.drawable.mw24_ic_ccd)
                    .into(iconImageView)
            }

            is WithThumbnail -> {
                val originalUrl = token.thumbnailUrl
                val previousRequest = thumbnailRequest

                // Keep successful or running requests for the same URL.
                // Failed or cleared requests can be retried.
                if (
                    originalUrl == lastThumbnailUrl &&
                    previousRequest != null &&
                    thumbnailTarget?.request === previousRequest &&
                    (previousRequest.isComplete || previousRequest.isRunning)
                ) {
                    return
                }

                resetThumbnailState()
                lastThumbnailUrl = originalUrl

                val urls = originalUrl?.let {
                    IpfsGatewayFallback.getUrls(it)
                } ?: emptyList()

                val target = buildImageRequest(urls)
                    .placeholder(loadingPlaceholder)
                    .error(R.drawable.mw24_ic_token_placeholder)
                    .into(iconImageView)

                thumbnailTarget = target
                thumbnailRequest = target.request
            }

            else -> {
                resetThumbnailState()

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

        var networkRequest: RequestBuilder<Drawable>? = null

        for (url in urls.asReversed()) {
            val nextRequest = Glide.with(context)
                .asDrawable()
                .load(url)
                .override(iconSize)
                .fitCenter()

            if (networkRequest == null) {
                // Display the token placeholder if every gateway fails.
                nextRequest.error(R.drawable.mw24_ic_token_placeholder)
            } else {
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
