package com.concordium.wallet.ui.onramp

import com.concordium.wallet.BuildConfig

class CcdOnrampSiteRepository {
    private val mainnetSites = listOf(
        CcdOnrampSite(
            name = "Swipelux",
            url = "https://swipelux.com/buy_ccd",
            logoUrl = "https://assets-global.website-files.com/64f060f3fc95f9d2081781db/65e825be9290e43f9d1bc29b_52c3517d-1bb0-4705-a952-8f0d2746b4c5.jpg",
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "LetsExchange",
            url = "https://letsexchange.io/",
            logoUrl = "https://assets-global.website-files.com/64f060f3fc95f9d2081781db/64fed810c7dd2cfc068c17cf_1680692222678%20(1).jpg",
            type = CcdOnrampSite.Type.CEX,
        ),
        CcdOnrampSite(
            name = "KuCoin",
            url = "https://www.kucoin.com/",
            logoUrl = "https://assets-global.website-files.com/64f060f3fc95f9d2081781db/64f0d1d17b59787f0d1b3740_logo-favicon-kucoin.png",
            type = CcdOnrampSite.Type.CEX,
        ),
        CcdOnrampSite(
            name = "Bitfinex",
            url = "https://trading.bitfinex.com/t/CCD:USD?type=exchange",
            logoUrl = "https://assets-global.website-files.com/64f060f3fc95f9d2081781db/64f0d15bd065ec0e03a32ac5_bitfinex.png",
            type = CcdOnrampSite.Type.CEX,
        ),
        CcdOnrampSite(
            name = "AscendEX (BitMax)",
            url = "https://ascendex.com/en/cashtrade-spottrading/usdt/ccd",
            logoUrl = "https://assets-global.website-files.com/64f060f3fc95f9d2081781db/64f0d273cc264cf97db45e72_logo-favicon-ascendex.png",
            type = CcdOnrampSite.Type.CEX,
        ),
        CcdOnrampSite(
            name = "MEXC",
            url = "https://www.mexc.com/",
            logoUrl = "https://assets-global.website-files.com/64f060f3fc95f9d2081781db/64f0d2446607fca14ba4af27_logo-favicon-mexc.png",
            type = CcdOnrampSite.Type.CEX,
        ),
        CcdOnrampSite(
            name = "Bit2Me",
            url = "https://bit2me.com/price/concordium",
            logoUrl = "https://assets-global.website-files.com/64f060f3fc95f9d2081781db/64f0d2d9a6d5f9d1cca6dfe6_logo-favicon-bit2me.png",
            type = CcdOnrampSite.Type.CEX,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "LCX",
            url = "https://exchange.lcx.com/",
            logoUrl = "https://assets-global.website-files.com/64f060f3fc95f9d2081781db/660ef2975d8736c5529f54b9_LCX.jpg",
            type = CcdOnrampSite.Type.CEX,
        ),
    )

    val hasSites: Boolean
        get() = getSites().isNotEmpty()

    fun getSites(): List<CcdOnrampSite> =
        when {
            BuildConfig.DEBUG || BuildConfig.ENV_NAME == "production" ->
                mainnetSites

            else ->
                emptyList()
        }
}
