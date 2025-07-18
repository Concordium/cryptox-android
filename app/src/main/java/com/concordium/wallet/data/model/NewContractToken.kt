package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ContractToken
import java.io.Serializable
import java.math.BigInteger

data class NewContractToken(
    override var balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String,
    override val isNewlyReceived: Boolean,
    override val addedAt: Long,
    override val type: TokenType = TokenType.CIS2,
    override val metadata: TokenMetadata?,
    override val isSelected: Boolean = false,
    val contractIndex: String,
    val subIndex: String = "0",
    val contractName: String,
    val token: String,
    val isFungible: Boolean,
) : NewToken, Serializable

fun ContractToken.toNewContractToken(
    balance: BigInteger = BigInteger.ZERO,
    isSelected: Boolean = false
) = NewContractToken(
    balance = balance,
    accountAddress = accountAddress ?: "",
    isNewlyReceived = isNewlyReceived,
    addedAt = addedAt,
    type = TokenType.CIS2,
    metadata = tokenMetadata,
    isSelected = isSelected,
    contractIndex = contractIndex,
    contractName = contractName,
    token = token,
    isFungible = isFungible
)
