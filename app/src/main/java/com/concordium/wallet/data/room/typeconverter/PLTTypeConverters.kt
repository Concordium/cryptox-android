package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.App
import com.concordium.wallet.data.model.TokenMetadata

class PLTTypeConverters {
    private val gson = App.appCore.gson

    @TypeConverter
    fun jsonToTokenMetadata(value: String): TokenMetadata? {
        return gson.fromJson(value, TokenMetadata::class.java)
    }

    @TypeConverter
    fun tokenMetadataToJson(tokenMetadata: TokenMetadata?): String {
        return gson.toJson(tokenMetadata)
    }
}