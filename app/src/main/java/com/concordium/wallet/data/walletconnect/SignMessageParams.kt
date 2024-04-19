package com.concordium.wallet.data.walletconnect

import com.concordium.wallet.App
import com.google.gson.JsonSyntaxException

sealed interface SignMessageParams {
    class Text(
        val data: String,
    ) : SignMessageParams

    class Binary(
        val schema: Schema,
        /**
         * Hex-encoded binary data.
         */
        val data: String,
    )

    companion object {
        private val gson by lazy {
            App.appCore.gson.newBuilder()
                .registerTypeAdapter(Schema::class.java, SchemaDeserializer())
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
