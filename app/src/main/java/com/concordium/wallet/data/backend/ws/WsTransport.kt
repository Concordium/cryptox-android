package com.concordium.wallet.data.backend.ws

import androidx.lifecycle.asLiveData
import com.concordium.wallet.App
import com.concordium.wallet.data.model.AccountInfo
import com.concordium.wallet.data.model.WsMessageResponse
import com.concordium.wallet.ui.connect.model.ConnectionAccept
import com.concordium.wallet.ui.connect.model.CreateNft
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tinder.scarlet.Message
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

data class WsCreds(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("connect_id")
    val connectId: Long
)

object WsTransport {

    private var bridge: IWsBridge? = null
    private val gson by lazy {
        Gson()
    }

    fun connect(url: String, isReconnect: Boolean = false): WsTransport {
        val endpoint = if (isReconnect) {
            "wss://cwb.spaceseven.cloud/ws/mobile/reconnect"
        } else {
            url
        }
        val okHttpClient = OkHttpClient.Builder().build()
        val ws = Scarlet.Builder().webSocketFactory(okHttpClient.newWebSocketFactory(endpoint))
            .addMessageAdapterFactory(GsonMessageAdapter.Factory())
            .addStreamAdapterFactory(CoroutinesStreamAdapterFactory())
            .build()
        try {
            bridge = ws.create(IWsBridge::class.java)
        } catch (ex: Exception) {
            println(">>>>><<<<<<<< $ex")
        }
        return this
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun subscribe(callback: (WsMessageResponse?, Throwable?) -> Unit) =
        GlobalScope.launch(Dispatchers.Main) {
            bridge?.observeWebSocketEvent()?.consumeAsFlow()?.asLiveData()
                ?.observeForever { event ->
                    try {
                        when (event) {
                            is WebSocket.Event.OnConnectionOpened<*> -> {
                            }
                            is WebSocket.Event.OnMessageReceived -> {
                                val msg = (event.message as Message.Text).value
                                when {
                                    msg.startsWith("proxy#") -> {
                                        val m2 = gson.fromJson(
                                            msg.substringAfter("#"),
                                            WsMessageResponse::class.java
                                        )
                                        callback(m2, null)
                                    }
                                    msg.startsWith("access#") -> {
                                        App.wsCreds = gson.fromJson(
                                            msg.substringAfter("#"),
                                            WsCreds::class.java
                                        )
                                    }
                                    else -> {
                                        println()
                                    }
                                }
                                println(">>>>>>>>>>> WsTransport incoming OnMessageReceived: ${event.message}")
                            }
                            is WebSocket.Event.OnConnectionClosing -> {
                                println(">>>>>>>>>>> WsTransport incoming OnConnectionClosing $event")
                            }
                            is WebSocket.Event.OnConnectionClosed -> {
                                println(">>>>>>>>>>> WsTransport incoming OnConnectionClosed $event")
                            }
                            is WebSocket.Event.OnConnectionFailed -> {
//                        callback(null, Throwable("ConnectionFailed"))
                                println(">>>>>>>>>>> WsTransport incoming OnConnectionFailed $event")
                            }
                        }
                    } catch (ex: Exception) {
                        callback(null, UnsupportedOperationException("Unsupported operation"))
                    }
                }
        }

    fun sendConnectionAccept() {
        val msg = ConnectionAccept(userStatus = "UserAccepted")
        println(">>>>>>>>>>> WsTransport outgoing sendConnectionAccept $msg")
        bridge?.sendConnectionAccept(msg)
    }

    fun sendConnectionReject() {
        val msg = ConnectionAccept(userStatus = "UserRejected")
        println(">>>>>>>>>>> WsTransport outgoing sendConnectionReject $msg")
        bridge?.sendConnectionAccept(msg)
    }

    fun sendAccountInfo(msg: AccountInfo) {
        println(">>>>>>>>>>> WsTransport outgoing sendAccountInfo $msg")
        bridge?.sendAccountInfo(msg)
    }

    fun sendTransactionResult(txHash: String? = null, action: String? = null) {
        val data = CreateNft.Data(
            txHash = txHash,
            txStatus = if (txHash == null) "Rejected" else "Accepted",
            action = action
        )
        val msg = CreateNft(data = data)
        bridge?.sendNftResult(msg)
        println(">>>>>>>>>>> WsTransport outgoing sendTransactionResult $msg")
    }
}
