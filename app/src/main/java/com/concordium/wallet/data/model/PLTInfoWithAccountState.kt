package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ProtocolLevelToken

data class PLTInfoWithAccountState(
    val token: PLTInfo,
    val tokenAccountState: TokenAccountState? = null
)

fun PLTInfoWithAccountState.toProtocolLevelToken(
    accountAddress: String,
    addedAt: Long = System.currentTimeMillis(),
    isHidden: Boolean = false,
    isNewlyReceived: Boolean = false
): ProtocolLevelToken {
    return ProtocolLevelToken(
        tokenId = token.tokenId,
        tokenState = token.tokenState,
        tokenAccountState = tokenAccountState,
        accountAddress = accountAddress,
        addedAt = addedAt,
        isHidden = isHidden,
        isNewlyReceived = isNewlyReceived
    )
}
