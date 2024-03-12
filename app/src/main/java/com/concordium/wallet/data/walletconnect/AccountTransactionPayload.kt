package com.concordium.wallet.data.walletconnect

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

sealed interface AccountTransactionPayload {
    data class Update(
        val address: ContractAddress,
        val amount: BigInteger,
        var maxEnergy: Long,
        val message: String,
        val receiveName: String
    ): AccountTransactionPayload

    data class Transfer(
        val amount: BigInteger,
        @SerializedName("to", alternate = ["toAddress"])
        val toAddress: String
    ): AccountTransactionPayload

    // The remaining type, InitContract, is not needed for now.
}
