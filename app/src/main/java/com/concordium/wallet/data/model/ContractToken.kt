package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ContractTokenEntity
import java.math.BigInteger

data class ContractToken(
    override var balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String = "",
    override val isNewlyReceived: Boolean = false,
    override val addedAt: Long = System.currentTimeMillis(),
    override var metadata: TokenMetadata? = null,
    override var isSelected: Boolean = false,
    var uid: String = "",
    var contractIndex: String = "",
    var subIndex: String = "0",
    var contractName: String = "",
    val token: String = "",
    val isFungible: Boolean = false,
) : Token {

    override val symbol: String
        get() = metadata?.symbol ?: ""

    val isUnique: Boolean
        get() = metadata?.unique == true
}

fun ContractTokenEntity.toContractToken(
    balance: BigInteger = BigInteger.ZERO,
    isSelected: Boolean = false,
) = ContractToken(
    balance = balance,
    accountAddress = accountAddress ?: "",
    isNewlyReceived = isNewlyReceived,
    addedAt = addedAt,
    metadata = tokenMetadata,
    isSelected = isSelected,
    uid = id.toString(),
    contractIndex = contractIndex,
    contractName = contractName,
    token = token,
    isFungible = isFungible
)
