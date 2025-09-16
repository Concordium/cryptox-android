package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ProtocolLevelTokenEntity

class PLTInfoWithAccountState(
    val token: PLTInfo,
    val tokenAccountState: TokenAccountState,
) {

    fun toProtocolLevelTokenEntity(
        accountAddress: String,
        metadata: ProtocolLevelTokenMetadata? = null,
    ) = ProtocolLevelTokenEntity(
        name = token.tokenState.moduleState.name,
        decimals = token.tokenState.decimals,
        tokenId = token.tokenId,
        accountAddress = accountAddress,
        balance = tokenAccountState.balance.value,
        metadata = metadata,
        addedAt = System.currentTimeMillis(),
        isHidden = false,
        isNewlyReceived = true,
        isInAllowList = tokenAccountState.state.allowList,
        isInDenyList = tokenAccountState.state.denyList,
        isPaused = token.tokenState.moduleState.paused
    )
}
