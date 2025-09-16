package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.ContractTokenEntity
import java.math.BigInteger

data class ContractToken(
    override var balance: BigInteger = BigInteger.ZERO,
    override val accountAddress: String = "",
    override val isNewlyReceived: Boolean = false,
    override val addedAt: Long = System.currentTimeMillis(),
    override var isSelected: Boolean = false,
    var uid: String = "",
    var contractIndex: String = "",
    var subIndex: String = "0",
    var contractName: String = "",
    /**
     * A token identifier within the contract.
     * It is empty for the first token on the contract,
     * otherwise a hexadecimal counter.
     */
    val token: String = "",
    var metadata: ContractTokenMetadata? = null,
) : Token, WithThumbnail {

    override val symbol: String
        get() = metadata?.symbol ?: ""

    override val decimals: Int
        get() = metadata?.decimals ?: 0

    override val thumbnailUrl: String?
        get() =
            metadata
                ?.thumbnail
                ?.url
                ?.takeIf(String::isNotBlank)

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
    metadata = metadata,
    isSelected = isSelected,
    uid = id.toString(),
    contractIndex = contractIndex,
    contractName = contractName,
    token = token,
)
