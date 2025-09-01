package com.concordium.wallet.data.backend.ws

import com.concordium.wallet.data.model.AccountInfo
import com.concordium.wallet.ui.connect.model.ConnectionAccept
import com.concordium.wallet.ui.connect.model.CreateNft
import com.google.gson.annotations.SerializedName
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.channels.ReceiveChannel

data class MessageFrame(
    @SerializedName("i")
    val sequence: Int = 0,
    @SerializedName("m")
    val messageType: MessageType = MessageType.REQUEST,
    @SerializedName("n")
    val functionName: String = "", // function name
    @SerializedName("o")
    val payload: String
)

enum class MessageType {
    @SerializedName("0")
    REQUEST,

    @SerializedName("1")
    REPLY,

    @SerializedName("2")
    SUBSCRIBE_TO_EVENT,

    @SerializedName("3")
    EVENT,

    @SerializedName("4")
    UNSUBSCRIBE_FROM_EVENT,

    @SerializedName("5")
    ERROR
}

interface IWsBridge {

    @Receive
    fun observeWebSocketEvent(): ReceiveChannel<WebSocket.Event>

    @Send
    fun sendAccountInfo(txt: AccountInfo)

    @Send
    fun sendConnectionAccept(msg: ConnectionAccept)

    @Send
    fun sendNftResult(msg: CreateNft)
}
