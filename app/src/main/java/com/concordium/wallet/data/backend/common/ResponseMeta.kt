package com.concordium.wallet.data.backend.common

import com.google.gson.annotations.SerializedName

data class ResponseMeta(
    @SerializedName("status")
    val status: Status,
    @SerializedName("error")
    val error: Error
) {

    data class Error(
        @SerializedName("messages")
        val messages: List<String>,
        @SerializedName("code")
        val code: String
    )

    enum class Status {
        UNSPECIFIED,
        OK,
        UNAUTHORIZED,
        FORBIDDEN,
        FAILEDPRECONDITION,
        SYSTEMERROR,
        NOTFOUND
    }
}