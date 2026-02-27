package com.concordium.wallet.data.walletconnect

import com.concordium.sdk.transactions.tokens.TransferTokenOperation
import com.concordium.sdk.types.ContractAddress
import com.google.gson.annotations.SerializedName
import java.math.BigInteger

sealed interface AccountTransactionPayload {

    data class Update(
        val address: ContractAddress,
        val amount: BigInteger,
        /**
         * Energy for the whole transaction including the administrative fee.
         * Legacy field.
         */
        val maxEnergy: Long?,
        /**
         * Energy for the smart contract execution only,
         * without the administrative transaction fee.
         */
        val maxContractExecutionEnergy: Long?,
        val message: String,
        val receiveName: String
    ): AccountTransactionPayload

    data class Transfer(
        val amount: BigInteger,
        @SerializedName("to", alternate = ["toAddress"])
        val toAddress: String
    ): AccountTransactionPayload

    data class PltTransfer(
        val tokenId: String,
        val transfer: TransferTokenOperation,
    ): AccountTransactionPayload

    // InitContract is not needed for now.
}
