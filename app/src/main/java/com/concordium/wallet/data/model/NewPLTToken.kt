package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ProtocolLevelToken
import java.io.Serializable
import java.math.BigInteger

data class NewPLTToken(
    override val balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String,
    override val isNewlyReceived: Boolean = false,
    override val addedAt: Long,
    override val type: TokenType = TokenType.PLT,
    override val metadata: TokenMetadata? = null,
    override val isSelected: Boolean = false,
    val tokenId: String,
    val tokenState: PLTState?,
    val tokenAccountState: TokenAccountState? = null,
    val isHidden: Boolean = false,
) : NewToken, Serializable

fun ProtocolLevelToken.toNewPLTToken(
    balance: BigInteger = BigInteger.ZERO,
    isSelected: Boolean = false,
    isNewlyReceived: Boolean = false,
) = NewPLTToken(
    balance = balance,
    accountAddress = accountAddress ?: "",
    isNewlyReceived = isNewlyReceived,
    addedAt = addedAt,
    type = TokenType.PLT,
    metadata = null, // PLT tokens do not have metadata
    isSelected = isSelected,
    tokenId = tokenId,
    tokenState = tokenState,
    tokenAccountState = tokenAccountState,
    isHidden = isHidden
)

