package com.concordium.wallet.ui.onramp

import com.concordium.wallet.BuildConfig

class CcdOnrampSiteRepository {
    private val mainnetSites = listOf(
        CcdOnrampSite(
            name = "Banxa",
            // Base URL.
            url = "https://concordium.banxa.com/",
            logoUrl = "https://files.readme.io/b031bd1-small-Banxa_Icon_RGB.png",
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "Swipelux",
            url = "https://track.swipelux.com",
            logoUrl = "https://assets-global.website-files.com/64f060f3fc95f9d2081781db/65e825be9290e43f9d1bc29b_52c3517d-1bb0-4705-a952-8f0d2746b4c5.jpg",
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "Wert",
            url = "https://widget.wert.io/01HM0W8FTFG4TEBRB0JPM18G5W/widget/?commodity=CCD&network=concordium&commodity_id=ccd.simple.concordium",
            logoUrl = "https://partner.wert.io/icons/apple-touch-icon.png",
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
    )

    private val testnetSites = listOf(
        CcdOnrampSite(
            name = "Banxa Sandbox",
            // Base URL.
            url = "https://concordium.banxa-sandbox.com/",
            logoUrl = "https://files.readme.io/b031bd1-small-Banxa_Icon_RGB.png",
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "Testnet CCD drop",
            url = "https://radiokot.github.io/ccd-faucet/",
            logoUrl = "https://em-content.zobj.net/source/apple/391/smiling-face-with-sunglasses_1f60e.png",
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
        ),
    )

    fun getSites(): List<CcdOnrampSite> =
        when {
            BuildConfig.DEBUG ->
                testnetSites + mainnetSites

            BuildConfig.ENV_NAME == "production" ->
                mainnetSites

            BuildConfig.ENV_NAME == "testnet" ->
                testnetSites

            else ->
                emptyList()
        }
}
