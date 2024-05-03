package com.concordium.wallet.data.walletconnect

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class SignMessageParamsDeserializer : JsonDeserializer<SignMessageParams?> {
    /**
     * Get the [SignMessageParamsDeserializer] from the [JsonElement]
     *
     * @return [SignMessageParams]
     *
     * **null** if [json] is null
     *
     * **null** if `message` value within the [json] is null
     */
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): SignMessageParams? {
        if (json == null || json.isJsonNull) return null

        val messageElement = json.asJsonObject["message"]
        return when {
            messageElement == null || messageElement.isJsonNull ->
                null

            messageElement.isJsonPrimitive ->
                SignMessageParams.Text(
                    data = messageElement.asString,
                )

            messageElement.isJsonObject ->
                SignMessageParams.Binary(
                    data = messageElement.asJsonObject["data"].asString,
                    schema = Schema.ValueSchema(
                        // For binary params deserialization, the parameter v1 schema is used.
                        type = "parameter",
                        value = messageElement.asJsonObject["schema"].asString,
                        version = 1,
                    ),
                )

            else ->
                throw JsonParseException("The 'message' element has unknown type")
        }
    }
}
