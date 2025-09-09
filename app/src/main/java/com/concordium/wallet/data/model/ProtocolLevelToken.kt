package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ProtocolLevelTokenEntity
import java.math.BigInteger

data class ProtocolLevelToken(
    override val decimals: Int,
    override val balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String,
    override val isNewlyReceived: Boolean = false,
    override val addedAt: Long,
    override var isSelected: Boolean = false,
    val tokenId: String,
    val name: String?,
    val isHidden: Boolean = false,
    val isInAllowList: Boolean? = null,
    val isInDenyList: Boolean? = null,
    val isPaused: Boolean = false,
    var metadata: ProtocolLevelTokenMetadata? = null,
) : Token, WithThumbnail {

    override val symbol: String
        get() = tokenId

    override val thumbnailUrl: String?
        get() =
            metadata
                ?.thumbnail
                ?.url
                ?.takeIf(String::isNotBlank)

    val isTransferable: Boolean
        get() = isInDenyList != true && isInAllowList != false && !isPaused
}

fun ProtocolLevelTokenEntity.toProtocolLevelToken(
    isSelected: Boolean = true,
) = ProtocolLevelToken(
    balance = balance,
    name = name,
    decimals = decimals,
    accountAddress = accountAddress ?: "",
    isNewlyReceived = isNewlyReceived,
    addedAt = addedAt,
    isSelected = isSelected,
    tokenId = tokenId,
    isHidden = isHidden,
    isInAllowList = isInAllowList,
    isInDenyList = isInDenyList,
    isPaused = isPaused,
)

