package com.concordium.wallet.data.walletconnect

import com.concordium.wallet.core.gson.NullableTypeAdapterFactory
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.io.Serializable

data class Params(
    val type: TransactionType,
    val sender: String,
    var payload: String?,
    var message: String?,
    var schema: Schema?
) : Serializable {
    fun parsePayload(): Payload? {
        return try {
            val gson = GsonBuilder()
                .registerTypeAdapterFactory(NullableTypeAdapterFactory())
                .create()
            gson.fromJson(payload, Payload::class.java)
        } catch (ex: java.lang.Exception) {
            Log.e(ex.toString())
            null
        }
    }

    companion object {
        /**
         * @return parsed [Params]
         *
         * @throws JsonSyntaxException if the request params can't be parsed
         */
        fun fromSessionRequestParams(sessionRequestParams: String): Params {
            val gson = GsonBuilder()
                .registerTypeAdapter(
                    Params::class.java,
                    ParamsDeserializer()
                )
                .create()

            return gson.fromJson(sessionRequestParams, Params::class.java)
        }
    }
}
