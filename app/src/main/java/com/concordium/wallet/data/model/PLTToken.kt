package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ProtocolLevelToken
import java.io.Serializable
import java.math.BigInteger

data class PLTToken(
    override val balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String,
    override val isNewlyReceived: Boolean = false,
    override val addedAt: Long,
    override val metadata: TokenMetadata? = null,
    override val isSelected: Boolean = false,
    val tokenId: String,
    val tokenState: PLTState?,
    val tokenAccountState: TokenAccountState? = null,
    val isHidden: Boolean = false,
    val isInAllowList: Boolean? = null,
    val isInDenyList: Boolean? = null,
) : NewToken, Serializable

fun ProtocolLevelToken.toNewPLTToken(
    isSelected: Boolean = false,
    isNewlyReceived: Boolean = false,
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

