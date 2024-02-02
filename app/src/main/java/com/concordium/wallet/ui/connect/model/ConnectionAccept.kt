package com.concordium.wallet.ui.connect.model

import com.concordium.wallet.BuildConfig
import com.concordium.wallet.ui.connect.WsMessage
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ConnectionAccept(
    val originator: String = "CryptoX Wallet Android app",
    @SerializedName("network_id")
    val networkId: String = BuildConfig.FLAVOR,
    val data: String = "ConnectionAcceptedNotification",
    @SerializedName("message_type")
    val messageType: String = "ConnectionAcceptNotify",
    @SerializedName("user_status")
    val userStatus: String = ""
) : WsMessage {
    fun toJson(): String = Gson().toJson(this)
    override fun toString(): String {
        return toJson()
    }
}
