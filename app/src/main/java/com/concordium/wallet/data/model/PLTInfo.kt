package com.concordium.wallet.data.model

class PLTInfo(
    val tokenId: String,
    val tokenState: PLTState,
)

fun PLTInfo.toProtocolLevelToken(
    accountAddress: String,
    isSelected: Boolean = false,
): ProtocolLevelToken {
    return ProtocolLevelToken(
        name = tokenState.moduleState.name,
        decimals = tokenState.decimals,
        accountAddress = accountAddress,
        addedAt = System.currentTimeMillis(),
        tokenId = tokenId,
        isSelected = isSelected,
        isPaused = tokenState.moduleState.paused,
        isInAllowList =
        if (tokenState.moduleState.allowList == true)
            false
        else
            null
    )
}
