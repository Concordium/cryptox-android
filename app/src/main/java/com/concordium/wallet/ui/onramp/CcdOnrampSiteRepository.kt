package com.concordium.wallet.ui.onramp

import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R

class CcdOnrampSiteRepository {
    private val mainnetSites = listOf(
        CcdOnrampSite(
            name = "Banxa",
            // Base URL.
            url = "https://concordium.banxa.com/",
            logo = LogoSource.Remote("https://cdn.prod.website-files.com/67d7fbcd510cf4a3a6267957/685a651d86ccc21ad06deb1b_banxa.jpg"),
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "Transak",
            // Base URL
            url = "https://api-gateway.transak.com",
            logo = LogoSource.Local(R.drawable.mw24_ic_transak_logomark),
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "Swipelux",
            url = "https://track.swipelux.com",
            logo = LogoSource.Remote("https://assets-global.website-files.com/64f060f3fc95f9d2081781db/65e825be9290e43f9d1bc29b_52c3517d-1bb0-4705-a952-8f0d2746b4c5.jpg"),
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "Wert",
            url = "https://widget.wert.io/01HM0W8FTFG4TEBRB0JPM18G5W/widget/?commodity=CCD&network=concordium&commodity_id=ccd.simple.concordium",
            logo = LogoSource.Remote("https://partner.wert.io/icons/apple-touch-icon.png"),
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
    )

    private val testnetSites = listOf(
        CcdOnrampSite(
            name = "Banxa Sandbox",
            // Base URL.
            url = "https://concordium.banxa-sandbox.com/",
            logo = LogoSource.Remote("https://cdn.prod.website-files.com/67d7fbcd510cf4a3a6267957/685a651d86ccc21ad06deb1b_banxa.jpg"),
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "Transak Testnet",
            // Base URL
            url = "https://api-gateway.transak.com",
            logo = LogoSource.Local(R.drawable.mw24_ic_transak_logomark),
            type = CcdOnrampSite.Type.PAYMENT_GATEWAY,
            acceptsCreditCard = true,
        ),
        CcdOnrampSite(
            name = "Testnet CCD drop",
            url = "https://radiokot.github.io/ccd-faucet/",
            logo = LogoSource.Remote("https://em-content.zobj.net/source/apple/391/smiling-face-with-sunglasses_1f60e.png"),
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
