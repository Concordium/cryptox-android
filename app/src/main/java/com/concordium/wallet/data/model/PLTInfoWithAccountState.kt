package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ProtocolLevelToken
import java.math.BigInteger

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
        tokenMetadata = TokenMetadata(
            decimals = token.tokenState?.decimals ?: 0,
            description = token.tokenState?.moduleState?.name ?: "",
            name = null,
            symbol = null,
            thumbnail = token.tokenState?.moduleState?.metadata?.url?.let { UrlHolder(it) },
            unique = false,
            display = null
        ),
//        tokenState = token.tokenState,
//        tokenAccountState = tokenAccountState,
        accountAddress = accountAddress,
        balance = tokenAccountState?.balance?.value ?: BigInteger.ZERO,
        addedAt = addedAt,
        isHidden = isHidden,
        isNewlyReceived = isNewlyReceived
    )
}
