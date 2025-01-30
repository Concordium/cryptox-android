package com.concordium.wallet.data.model

import com.concordium.sdk.crypto.ed25519.ED25519SecretKey
import com.concordium.sdk.transactions.Index
import com.concordium.sdk.transactions.SignerEntry
import com.concordium.wallet.App
import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class AccountData(
    @JsonAdapter(RawJsonTypeAdapter::class)
    val keys: RawJson,
    val threshold: Int,
) : Serializable {
    fun getSecreteKey(): ED25519SecretKey =
        App.appCore.gson.fromJson(keys.json, AccountDataKeys::class.java)
            .level0
            .keys
            .keys
            .signKey
            .let(ED25519SecretKey::from)

    fun getSignerEntry(): SignerEntry=
        SignerEntry.from(Index.from(0), Index.from(0), getSecreteKey())
}

