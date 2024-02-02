package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.model.AccountData
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class UpdateTransferInput(
    val amount: String,
    val from: String,
    val nonce: Int,
    val expiry: Long,
    val energy: Long,
    val keys: AccountData,
    val params: String,
    val receiveName: String,
    val contractAddress: ContractAddress
) {
    data class ContractAddress(
        val index: Int,
        @SerializedName("subindex")
        val subIndex: Int
    )

    override fun toString(): String {
        return Gson().toJson(this)
    }
}
