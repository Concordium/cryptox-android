package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.App
import com.concordium.wallet.data.model.ContractTokenMetadata

class ContractTypeConverters {
    @TypeConverter
    fun jsonToTokenMetadata(value: String?): ContractTokenMetadata? {
        val gson = App.appCore.gson
        return gson.fromJson(value, ContractTokenMetadata::class.java)
    }

    @TypeConverter
    fun tokenMetadataToJson(contractTokenMetadata: ContractTokenMetadata?): String {
        val gson = App.appCore.gson
        return gson.toJson(contractTokenMetadata)
    }
}
