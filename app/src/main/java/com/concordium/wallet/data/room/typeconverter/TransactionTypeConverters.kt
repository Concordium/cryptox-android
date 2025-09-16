package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.App
import com.concordium.wallet.data.model.TokenAmount
import com.concordium.wallet.data.model.TransactionType


class TransactionTypeConverters {

    @TypeConverter
    fun jsonToTransactionType(value: String?): TransactionType? {
        return value?.let {
            App.appCore.gson.fromJson(value, TransactionType::class.java)
        }
    }

    @TypeConverter
    fun transactionTypeToJson(type: TransactionType?): String? {
        return type?.let(App.appCore.gson::toJson)
    }

    @TypeConverter
    fun jsonToTokenAmount(value: String?): TokenAmount? {
        return value?.let {
            App.appCore.gson.fromJson(value, TokenAmount::class.java)
        }
    }

    @TypeConverter
    fun tokenAmountToJson(amount: TokenAmount?): String? {
        return amount?.let(App.appCore.gson::toJson)
    }
}
