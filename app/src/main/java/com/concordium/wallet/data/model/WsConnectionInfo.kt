package com.concordium.wallet.data.model

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

@kotlinx.parcelize.Parcelize
data class WsConnectionInfo(
    @SerializedName("site")
    val site: SiteInfo,
    @SerializedName("connection")
    val connection: Connection,
    @SerializedName("ws_conn")
    val wsConnLink: String
) : Parcelable {

    fun toJson(): String = Gson().toJson(this)

    @kotlinx.parcelize.Parcelize
    data class SiteInfo(
        @SerializedName("title")
        val title: String = "",
        @SerializedName("description")
        val description: String = "",
        @SerializedName("icon_link")
        val iconLink: String = ""
    ) : Parcelable {
        fun toJson(): String = Gson().toJson(this)
    }

    @kotlinx.parcelize.Parcelize
    data class Connection(
        @SerializedName("ip")
        val ip: String,
        @SerializedName("platform")
        val platform: String,
        @SerializedName("os")
        val os: String,
        @SerializedName("browser")
        val browser: String,
        @SerializedName("is_portable")
        val isPortable: Boolean,
        @SerializedName("opened_at")
        val openedAt: Long
    ) : Parcelable {
        fun toJson(): String = Gson().toJson(this)
    }
}
