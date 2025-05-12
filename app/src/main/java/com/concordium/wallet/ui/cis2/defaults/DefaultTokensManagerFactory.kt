package com.concordium.wallet.ui.cis2.defaults

import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.ContractTokensRepository

class DefaultTokensManagerFactory(
    private val contractTokensRepository: ContractTokensRepository,
) {
    private val testnetDefaultFungibleTokens: List<DefaultFungibleToken> = listOf(
        DefaultFungibleToken(
            symbol = "EUROe",
            name = "EUROe Stablecoin",
            contractIndex = "7260",
            contractName = "euroe_stablecoin_v3",
            description = "EUROe is a modern European stablecoin - a digital representation of fiat Euros",
            thumbnailUrl = "https://dev.euroe.com/persistent/token-icon/png/128x128.png",
            decimals = 6,
            token = "",
        ),
        DefaultFungibleToken(
            symbol = "wCCD",
            name = "Wrapped CCD Token",
            contractIndex = "2059",
            contractName = "cis2_wCCD",
            description = "A CIS2 token wrapping the Concordium native token (CCD)",
            thumbnailUrl = "https://developer.concordium.software/en/mainnet/_images/wCCD.svg",
            decimals = 6,
            token = "",
        ),
        DefaultFungibleToken(
            symbol = "DemoUSD",
            name = "DemoUSD",
            contractIndex = "11353",
            contractName = "mint_wizard_000000_V3",
            description = "USD token for demos",
            thumbnailUrl = null,
            decimals = 2,
            token = "01",
        ),
    )

    private val mainnetDefaultFungibleTokens: List<DefaultFungibleToken> = listOf(
        DefaultFungibleToken(
            symbol = "EUROe",
            name = "EUROe Stablecoin",
            contractIndex = "9390",
            contractName = "euroe_stablecoin",
            description = "EUROe is a modern European stablecoin - a digital representation of fiat Euros",
            thumbnailUrl = "https://dev.euroe.com/persistent/token-icon/png/128x128.png",
            decimals = 6,
            token = "",
        ),
        DefaultFungibleToken(
            symbol = "wCCD",
            name = "Wrapped CCD Token",
            contractIndex = "9354",
            contractName = "cis2_wCCD",
            description = "A CIS2 token wrapping the Concordium native token (CCD)",
            thumbnailUrl = "https://developer.concordium.software/en/mainnet/_images/wCCD.svg",
            decimals = 6,
            token = "",
        ),
    )

    fun getDefaultFungibleTokensManager() =
        DefaultFungibleTokensManager(
            defaults =
            when (BuildConfig.ENV_NAME) {
                "production" -> mainnetDefaultFungibleTokens
                "testnet" -> testnetDefaultFungibleTokens
                else -> emptyList()
            },
            contractTokensRepository = contractTokensRepository,
        )
}
