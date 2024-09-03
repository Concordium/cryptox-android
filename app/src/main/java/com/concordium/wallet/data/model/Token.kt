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
    var metadata: TokenMetadata? = null,
    var contractIndex: String = "",
    var subIndex: String = "0",
    var contractName: String = "",
    var balance: BigInteger = BigInteger.ZERO,
    var isSelected: Boolean = false,
    var isNewlyReceived: Boolean = false,
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
        get() = id == "CCD"

    constructor(
        contractToken: ContractToken,
        isSelected: Boolean = false,
    ) : this(
        id = contractToken.tokenId,
        token = contractToken.tokenId,
        metadata = contractToken.tokenMetadata,
        contractIndex = contractToken.contractIndex,
        contractName = contractToken.contractName,
        isNewlyReceived = contractToken.isNewlyReceived,
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
                id = "CCD",
                metadata = TokenMetadata(
                    symbol = "CCD",
                    decimals = 6,
                    unique = false,
                    name = null,
                    description = null,
                    thumbnail = null,
                    display = null,
                ),
                balance = atDisposal,
            )
        }
    }
}
