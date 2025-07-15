package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.App
import com.concordium.wallet.data.model.PLTState
import com.concordium.wallet.data.model.TokenAccountState

class PLTTypeConverters {
    private val gson = App.appCore.gson

    @TypeConverter
    fun jsonToPLTState(value: String?): PLTState? {
        if (value == null) {
           return null
        }
        return gson.fromJson(value, PLTState::class.java)
    }

    @TypeConverter
    fun pltStateToJson(pltState: PLTState?): String? {
        if (pltState == null) {
            return null
        }
        return gson.toJson(pltState)
    }

    @TypeConverter
    fun jsonToTokenAccountState(value: String?): TokenAccountState? {
        if (value == null) {
            return null
        }
        return gson.fromJson(value, TokenAccountState::class.java)
    }

    @TypeConverter
    fun tokenAccountStateToJson(tokenAccountState: TokenAccountState?): String? {
        if (tokenAccountState == null) {
            return null
        }
        return gson.toJson(tokenAccountState)
    }
}