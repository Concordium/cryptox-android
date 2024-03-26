package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.RawJson
import com.google.gson.Gson
import com.google.gson.annotations.JsonAdapter

data class StorageAccountData(
    val accountAddress: String,
    val accountKeys: AccountData,
    val encryptionSecretKey: String,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val commitmentsRandomness: RawJson? = null,
) {
    fun getAttributeRandomness(): AttributeRandomness? {
        return try {
            return Gson().fromJson(commitmentsRandomness!!.json, AttributeRandomness::class.java)
        } catch (ex: Exception) {
            null
        }
    }

}

data class AttributeRandomness(
    val attributesRand: Map<String, String>
)
