package com.concordium.wallet.ui.cis2.defaults

import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.model.UrlHolder
import com.concordium.wallet.data.room.ContractToken

class DefaultFungibleToken(
    val symbol: String,
    val contractIndex: String,
    val tokenId: String,
    val contractName: String,
    val name: String,
    val description: String,
    val thumbnailUrl: String?,
    val decimals: Int,
) {
    fun toContractToken(accountAddress: String) = ContractToken(
        id = 0,
        contractIndex = contractIndex,
        tokenId = tokenId,
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
        )
    )
}
