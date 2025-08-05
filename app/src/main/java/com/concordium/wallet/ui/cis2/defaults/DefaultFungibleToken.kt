package com.concordium.wallet.ui.cis2.defaults

import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.model.UrlHolder
import com.concordium.wallet.data.room.ContractTokenEntity

class DefaultFungibleToken(
    val symbol: String,
    val contractIndex: String,
    val contractName: String,
    val token: String,
    val name: String,
    val description: String,
    val thumbnailUrl: String?,
    val decimals: Int,
) {
    fun toContractToken(accountAddress: String) = ContractTokenEntity(
        id = 0,
        contractIndex = contractIndex,
        token = token,
        accountAddress = accountAddress,
        isFungible = true,
        contractName = contractName,
        tokenMetadata = TokenMetadata(
            decimals = decimals,
            description = description,
            name = name,
            symbol = symbol,
            thumbnail = UrlHolder(
                url = thumbnailUrl,
            ),
            unique = false,
            display = null,
            totalSupply = null
        ),
        isNewlyReceived = false,
    )
}
