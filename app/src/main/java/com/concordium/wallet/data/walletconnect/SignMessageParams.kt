package com.concordium.wallet.data.walletconnect

import com.concordium.wallet.App
import com.google.gson.JsonSyntaxException

sealed interface SignMessageParams {
    val data: String

    class Text(
        override val data: String,
    ) : SignMessageParams

    class Binary(
        val schema: Schema,
        /**
         * Hex-encoded binary data.
         */
        override val data: String,
    ): SignMessageParams

    companion object {
        private val gson by lazy {
            App.appCore.gson.newBuilder()
                .registerTypeAdapter(SignMessageParams::class.java, SignMessageParamsDeserializer())
                .create()
        }

        /**
         * @return parsed [SignMessageParams]
         *
         * @throws JsonSyntaxException if the request params can't be parsed
         */
        fun fromSessionRequestParams(sessionRequestParams: String): SignMessageParams =
            gson.fromJson(sessionRequestParams, SignMessageParams::class.java)
    }
}
