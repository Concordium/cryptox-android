package com.concordium.wallet.data.walletconnect

import com.concordium.wallet.App
import com.concordium.wallet.core.gson.NullableTypeAdapterFactory
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.util.Log
import com.google.gson.JsonSyntaxException
import java.io.Serializable

data class AccountTransactionParams(
    val type: TransactionType,
    val sender: String,
    var payload: String?,
    var schema: Schema?
) : Serializable {
    fun parsePayload(): AccountTransactionPayload? = try {
        val gson = App.appCore.gson.newBuilder()
            .registerTypeAdapterFactory(NullableTypeAdapterFactory())
            .create()

        when (type) {
            TransactionType.TRANSFER ->
                gson.fromJson(payload, AccountTransactionPayload.Transfer::class.java)

            TransactionType.UPDATE ->
                gson.fromJson(payload, AccountTransactionPayload.Update::class.java)

            else ->
                error("Can't parse payload for unsupported type: $type")
        }
    } catch (ex: Throwable) {
        Log.e(ex.toString())
        null
    }

    companion object {
        private val gson by lazy {
            App.appCore.gson.newBuilder()
                .registerTypeAdapter(Schema::class.java, SchemaDeserializer())
                .create()
        }

        /**
         * @return parsed [AccountTransactionParams]
         *
         * @throws JsonSyntaxException if the request params can't be parsed
         */
        fun fromSessionRequestParams(sessionRequestParams: String): AccountTransactionParams =
            gson.fromJson(sessionRequestParams, AccountTransactionParams::class.java)
    }
}
