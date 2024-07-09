package com.concordium.wallet.data.model

import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.ContractToken
import java.io.Serializable
import java.math.BigInteger

// The class must have default value for each field,
// otherwise Gson fails to use defaults and sets not-nullable fields to null.
data class Token(
    val id: String = "",
    var token: String = "",
    val totalSupply: String = "",
    var tokenMetadata: TokenMetadata? = null,
    var isSelected: Boolean = false,
    var contractIndex: String = "",
    var subIndex: String = "0",
    var isCCDToken: Boolean = false,
    var totalBalance: BigInteger = BigInteger.ZERO,
    var atDisposal: BigInteger = BigInteger.ZERO,
    var contractName: String = "",
    var symbol: String = "",
) : Serializable {

    constructor(
        contractToken: ContractToken,
        isSelected: Boolean = false,
    ) : this(
        id = contractToken.tokenId,
        token = contractToken.tokenId,
        tokenMetadata = contractToken.tokenMetadata,
        contractIndex = contractToken.contractIndex,
        contractName = contractToken.contractName,
        symbol = contractToken.tokenMetadata?.symbol ?: "",
        isSelected = isSelected,
    )

    companion object {
        /**
         * @return CCD as if it was a fungible token,
         * with the [account]'s at disposal balance.
         */
        fun ccd(account: Account): Token {
            val totalUnshieldedBalance = account.totalUnshieldedBalance
            val atDisposal = account.getAtDisposalWithoutStakedOrScheduled(totalUnshieldedBalance)

            return Token(
                id = "",
                token = "CCD",
                symbol = "CCD",
                tokenMetadata = TokenMetadata(
                    symbol = "CCD",
                    decimals = 6,
                    unique = false,
                    name = null,
                    description = null,
                    thumbnail = null,
                    display = null,
                ),
                isCCDToken = true,
                isSelected = false,
                totalSupply = "",
                contractIndex = "",
                subIndex = "",
                totalBalance = totalUnshieldedBalance,
                atDisposal = atDisposal,
                contractName = "",
            )
        }
    }
}
