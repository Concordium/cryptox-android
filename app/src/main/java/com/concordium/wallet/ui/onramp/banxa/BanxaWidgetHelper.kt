package com.concordium.wallet.ui.onramp.banxa

object BanxaWidgetHelper {

    fun getWidgetLink(
        baseUrl: String,
        accountAddress: String,
    ): String =
        baseUrl
            .trim('/') +
                "?coinType=CCD" +
                "&walletAddress=$accountAddress" +
                "&orderType=buy"
}
