package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ProtocolLevelToken
import java.math.BigInteger

data class PLTToken(
    override val balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String,
    override val isNewlyReceived: Boolean = false,
    override val addedAt: Long,
    override val metadata: TokenMetadata? = null,
    override var isSelected: Boolean = false,
    val tokenId: String,
    val tokenState: PLTState?,
    val tokenAccountState: TokenAccountState? = null,
    val isHidden: Boolean = false,
    val isInAllowList: Boolean? = null,
    val isInDenyList: Boolean? = null,
) : NewToken {

    override val symbol: String
        get() = tokenId

    val isTransferable: Boolean
        get() = isInDenyList != true && isInAllowList != false
}

fun ProtocolLevelToken.toPLTToken(
    isSelected: Boolean = false,
) = PLTToken(
    balance = balance,
    accountAddress = accountAddress ?: "",
    isNewlyReceived = isNewlyReceived,
    addedAt = addedAt,
    metadata = tokenMetadata,
    isSelected = isSelected,
    tokenId = tokenId,
    tokenState = null,
    tokenAccountState = null,
    isHidden = isHidden,
    isInAllowList = isInAllowList,
    isInDenyList = isInDenyList
)

