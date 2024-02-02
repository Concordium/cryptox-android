package com.concordium.wallet.data.backend.airdrop

import com.google.gson.annotations.SerializedName

data class RegistrationRequest(
    @SerializedName("airdrop_id")
    val airdropId: Long,
    val language: String = "en",
    val address: Address
) {
    data class Address(
        val concordium: String
    )
}