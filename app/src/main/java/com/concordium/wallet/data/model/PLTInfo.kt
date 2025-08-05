package com.concordium.wallet.data.model

data class PLTInfo(
    val tokenId: String,
    val tokenState: PLTState?
)

fun PLTInfo.toProtocolLevelToken(
    accountAddress: String,
    isSelected: Boolean = false,
): ProtocolLevelToken {
    return ProtocolLevelToken(
        accountAddress = accountAddress,
        addedAt = System.currentTimeMillis(),
        tokenId = tokenId,
        tokenState = tokenState,
        metadata = TokenMetadata(
            decimals = tokenState?.decimals ?: 0,
            description = tokenState?.moduleState?.name,
            name = null,
            symbol = tokenId,
            thumbnail = UrlHolder(tokenState?.moduleState?.metadata?.url),
            unique = false,
            display = null,
            totalSupply = tokenState?.totalSupply?.value
        ),
        isSelected = isSelected,
        isInAllowList = if (tokenState?.moduleState?.allowList == true)
            false
        else
            null
    )
}
