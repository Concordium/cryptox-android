package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ContractToken
import java.math.BigInteger

data class NewContractToken(
    override var balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String,
    override val isNewlyReceived: Boolean,
    override val addedAt: Long,
    override val metadata: TokenMetadata?,
    override val isSelected: Boolean = false,
    val contractIndex: String,
    val subIndex: String = "0",
    val contractName: String,
    val token: String,
    val isFungible: Boolean,
) : NewToken {

    override val symbol: String
        get() = metadata?.symbol ?: ""

    val isUnique: Boolean
        get() = metadata?.unique == true
}

fun ContractToken.toNewContractToken(
    balance: BigInteger = BigInteger.ZERO,
    isSelected: Boolean = false,
) = NewContractToken(
    balance = balance,
    accountAddress = accountAddress ?: "",
    isNewlyReceived = isNewlyReceived,
    addedAt = addedAt,
    metadata = tokenMetadata,
    isSelected = isSelected,
    contractIndex = contractIndex,
    contractName = contractName,
    token = token,
    isFungible = isFungible
)
