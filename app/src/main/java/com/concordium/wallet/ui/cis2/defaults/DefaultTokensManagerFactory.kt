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
            symbol = "USDT.eth",
            name = "Tether USD (Arabella Bridge)",
            contractIndex = "4517",
            contractName = "CIS2-TOKEN",
            description = "Tether USD (Arabella Bridge)",
            thumbnailUrl = "https://cdn.moralis.io/eth/0xdac17f958d2ee523a2206206994597c13d831ec7.png",
            decimals = 6,
            token = "",
        ),
        DefaultFungibleToken(
            symbol = "USDC.eth",
            name = "Circle USD (Arabella Bridge)",
            contractIndex = "4516",
            contractName = "CIS2-TOKEN",
            description = "Circle USD (Arabella Bridge)",
            thumbnailUrl = "https://cdn.moralis.io/eth/0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48.png",
            decimals = 6,
            token = "",
        ),
        DefaultFungibleToken(
            symbol = "ETH.eth",
            name = "Ethereum (Arabella Bridge)",
            contractIndex = "4514",
            contractName = "CIS2-TOKEN",
            description = "Ethereum (Arabella Bridge)",
            thumbnailUrl = "https://cdn.moralis.io/eth/0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee.png",
            decimals = 18,
            token = "",
        ),
        DefaultFungibleToken(
            symbol = "WBTC.eth",
            name = "Wrapped Bitcoin (Arabella Bridge)",
            contractIndex = "4515",
            contractName = "CIS2-TOKEN",
            description = "Wrapped Bitcoin (Arabella Bridge)",
            thumbnailUrl = "https://cdn.moralis.io/eth/0x2260fac5e5542a773aa44fbcfedf7c193bc2c599.png",
            decimals = 8,
            token = "",
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
        DefaultFungibleToken(
            symbol = "USDT.eth",
            name = "Bridged USDT token",
            contractIndex = "9341",
            contractName = "cis2-bridgeable",
            description = "USDT token bridged from Ethereum",
            thumbnailUrl = "https://cryptologos.cc/logos/tether-usdt-logo.svg?v=025",
            decimals = 6,
            token = "",
        ),
        DefaultFungibleToken(
            symbol = "USDC.eth",
            name = "Bridged USDC token",
            contractIndex = "9339",
            contractName = "cis2-bridgeable",
            description = "USDC token bridged from Ethereum",
            thumbnailUrl = "https://cryptologos.cc/logos/usd-coin-usdc-logo.svg?v=024",
            decimals = 6,
            token = "",
        ),
        DefaultFungibleToken(
            symbol = "ETH.eth",
            name = "Bridged native Ethereum token",
            contractIndex = "9338",
            contractName = "cis2-bridgeable",
            description = "Bridged native Ethereum token",
            thumbnailUrl = "https://cryptologos.cc/logos/ethereum-eth-logo.svg?v=025",
            decimals = 18,
            token = "",
        ),
        DefaultFungibleToken(
            symbol = "WBTC.eth",
            name = "Bridged WBTC token",
            contractIndex = "9340",
            contractName = "cis2-bridgeable",
            description = "WBTC token bridged from Ethereum",
            thumbnailUrl = "https://cryptologos.cc/logos/wrapped-bitcoin-wbtc-logo.svg?v=025",
            decimals = 8,
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
