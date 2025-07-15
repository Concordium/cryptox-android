package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.ContractToken
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable
import java.math.BigInteger

// The class must have default value for each field,
// otherwise Gson fails to use defaults and sets not-nullable fields to null.
data class Token(
    /**
     * A unique identifier of this token.
     * Either a wallet-proxy id or a local [ContractToken.id] from the local DB.
     * **wallet-proxy "tokenId" is [token]**
     */
    @JsonProperty("id")
    val uid: String = "",
    /**
     * An identifier of this token **within its contract**,
     * either an empty string or a hex counter.
     */
    var token: String = "",
    var metadata: TokenMetadata? = null,
    var contractIndex: String = "",
    var subIndex: String = "0",
    var contractName: String = "",
    var balance: BigInteger = BigInteger.ZERO,
    var isSelected: Boolean = false,
    var isNewlyReceived: Boolean = false,
    var isEarning: Boolean = false,
    /**
     * Number of EUR per smallest token unit.
     */
    val eurPerUnit: SimpleFraction? = null,
    val type: TokenType = TokenType.UNKNOWN,
) : Serializable {

    val symbol: String
        get() = metadata?.symbol ?: ""

    val name: String?
        get() = metadata?.name

    val decimals: Int
        get() = metadata?.decimals ?: 0

    val isUnique: Boolean
        get() = metadata?.unique == true

    val isCcd: Boolean
        get() = uid == "CCD"

    constructor(
        contractToken: ContractToken,
        isSelected: Boolean = false,
        isEarning: Boolean = false,
    ) : this(
        uid = contractToken.id.toString(),
        token = contractToken.token,
        metadata = contractToken.tokenMetadata,
        contractIndex = contractToken.contractIndex,
        contractName = contractToken.contractName,
        isNewlyReceived = contractToken.isNewlyReceived,
        isSelected = isSelected,
        isEarning = isEarning,
        type = TokenType.CIS2
    )

    constructor(
        pltToken: PLTInfo,
        isSelected: Boolean = false,
        isEarning: Boolean = false,
    ) : this(
        uid = pltToken.tokenId,
        token = pltToken.tokenId,
        metadata = TokenMetadata(
            decimals = pltToken.tokenState?.decimals,
            description = null,
            name = pltToken.tokenId,
            symbol = pltToken.tokenId,
            thumbnail = pltToken.tokenState?.moduleState?.metadata?.url.let { UrlHolder(it) },
            unique = false,
            display = null
        ),
        contractIndex = "",
        contractName = "",
        isSelected = isSelected,
        isNewlyReceived = false,
        isEarning = isEarning,
        type = TokenType.PLT
    )

    companion object {
        /**
         * @return CCD as if it was a fungible token,
         * with the [account]'s at disposal balance.
         */
        fun ccd(
            account: Account,
            eurPerMicroCcd: SimpleFraction? = null,
        ) = Token(
            uid = "CCD",
            metadata = TokenMetadata(
                symbol = "CCD",
                decimals = 6,
                unique = false,
                name = null,
                description = null,
                thumbnail = null,
                display = null,
            ),
            balance = account.balance,
            isEarning = account.isBaking() || account.isDelegating(),
            eurPerUnit = eurPerMicroCcd,
            type = TokenType.CCD,
        )
    }
}
