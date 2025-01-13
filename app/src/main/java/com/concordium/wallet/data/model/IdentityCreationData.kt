package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

class IdentityCreationData(
    val identityProvider: IdentityProvider,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val idObjectRequest: RawJson,
    val identityName: String,
    val identityIndex: Int,
): Serializable
