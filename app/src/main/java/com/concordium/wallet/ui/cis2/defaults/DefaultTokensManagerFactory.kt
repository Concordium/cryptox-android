package com.concordium.wallet.ui.cis2.defaults

import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.ContractTokensRepository

class DefaultTokensManagerFactory(
    private val contractTokensRepository: ContractTokensRepository,
) {
    private val testnetDefaultFungibleTokens: List<DefaultFungibleToken> = listOf(
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
    )

    private val mainnetDefaultFungibleTokens: List<DefaultFungibleToken> = listOf(
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
