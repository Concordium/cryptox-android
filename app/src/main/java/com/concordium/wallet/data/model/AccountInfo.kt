package com.concordium.wallet.data.model

import com.concordium.wallet.BuildConfig
import com.concordium.wallet.ui.connect.WsMessage
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class AccountInfo(
    @SerializedName("originator")
    val originator: String = "CryptoX Wallet Android app",
    @SerializedName("network_id")
    val networkId: String = BuildConfig.FLAVOR,
    @SerializedName("data")
    val data: List<WalletInfo> = mutableListOf(),
    @SerializedName("message_type")
    val messageType: String = "AccountInfoResponse",
    @SerializedName("user_status")
    val userStatus: String = ""
) : WsMessage {

    fun toJson(): String = Gson().toJson(this)

    override fun toString(): String {
        return toJson()
    }

    data class WalletInfo(
        @SerializedName("address")
        val address: String,
        @SerializedName("balance")
        val balance: String
    )
}
