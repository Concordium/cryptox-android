package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.App
import com.concordium.wallet.data.model.ProtocolLevelTokenMetadata

class ProtocolLevelTokenTypeConverters {

    @TypeConverter
    fun metadataToJson(metadata: ProtocolLevelTokenMetadata?): String? {
        return metadata?.let(App.appCore.gson::toJson)
    }

    @TypeConverter
    fun jsonToMetadata(json: String?): ProtocolLevelTokenMetadata? {
        return json?.let {
            App.appCore.gson.fromJson(it, ProtocolLevelTokenMetadata::class.java)
        }
    }
}
