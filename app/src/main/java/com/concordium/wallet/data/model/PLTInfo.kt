package com.concordium.wallet.data.model

class PLTInfo(
    val tokenId: String,
    val tokenState: PLTState,
) {

    fun toProtocolLevelToken(
        accountAddress: String,
        metadata: ProtocolLevelTokenMetadata? = null,
        isNewlyReceived: Boolean = false,
        isSelected: Boolean = false,
    ) = ProtocolLevelToken(
        name = tokenState.moduleState.name,
        decimals = tokenState.decimals,
        accountAddress = accountAddress,
        addedAt = System.currentTimeMillis(),
        tokenId = tokenId,
        metadata = metadata,
        isNewlyReceived = isNewlyReceived,
        isSelected = isSelected,
        isPaused = tokenState.moduleState.paused,
        isInAllowList =
        if (tokenState.moduleState.allowList == true)
            false
        else
            null
    )
}
