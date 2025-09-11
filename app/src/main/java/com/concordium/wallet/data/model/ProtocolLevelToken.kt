package com.concordium.wallet.data.model

import java.math.BigInteger

data class ProtocolLevelToken(
    override val decimals: Int,
    override val balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String,
    override val isNewlyReceived: Boolean,
    override val addedAt: Long,
    override var isSelected: Boolean = false,
    val tokenId: String,
    val name: String?,
    val isHidden: Boolean,
    val isInAllowList: Boolean?,
    val isInDenyList: Boolean?,
    val isPaused: Boolean,
    val metadata: ProtocolLevelTokenMetadata?,
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
