package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ProtocolLevelTokenEntity

class PLTInfoWithAccountState(
    val token: PLTInfo,
    val tokenAccountState: TokenAccountState,
)

fun PLTInfoWithAccountState.toProtocolLevelTokenEntity(
    accountAddress: String,
    addedAt: Long = System.currentTimeMillis(),
    isHidden: Boolean = false,
    isNewlyReceived: Boolean = false,
): ProtocolLevelTokenEntity {
    return ProtocolLevelTokenEntity(
        name = token.tokenState.moduleState.name,
        decimals = token.tokenState.decimals,
        tokenId = token.tokenId,
        accountAddress = accountAddress,
        balance = tokenAccountState.balance.value,
        metadata = null,
        addedAt = addedAt,
        isHidden = isHidden,
        isNewlyReceived = isNewlyReceived,
        isInAllowList = tokenAccountState.state.allowList,
        isInDenyList = tokenAccountState.state.denyList,
    )
}
