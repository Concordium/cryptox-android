package com.concordium.wallet.ui.connect.model

import com.concordium.wallet.BuildConfig
import com.concordium.wallet.ui.connect.WsMessage
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class CreateNft(
    @SerializedName("originator")
    val originator: String = "CryptoX Wallet Android app",
    @SerializedName("network_id")
    val networkId: String = BuildConfig.FLAVOR,
    @SerializedName("data")
    val data: Data,
    @SerializedName("message_type")
    val messageType: String = "SimpleTransferResponse",
    @SerializedName("user_status")
    val userStatus: String = ""
) : WsMessage {
    fun toJson(): String = Gson().toJson(this)
    override fun toString(): String {
        return toJson()
    }

    data class Data(
        @SerializedName("tx_hash")
        val txHash: String? = null,
        @SerializedName("tx_status")
        val txStatus: String,
        @SerializedName("action")
        val action: String? = null
    )
}
