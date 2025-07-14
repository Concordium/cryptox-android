package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ProtocolLevelToken

data class PLTInfo(
    val tokenId: String,
    val tokenState: PLTState?
)

fun ProtocolLevelToken.toPltInfo(): PLTInfo {
    return PLTInfo(
        tokenId = this.tokenId,
        tokenState = this.tokenState
    )
}
