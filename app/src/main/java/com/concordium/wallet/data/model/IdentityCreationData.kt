package com.concordium.wallet.data.model

import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

sealed class IdentityCreationData(
    val identityProvider: IdentityProvider,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val idObjectRequest: RawJson,
    val identityName: String,
    val identityIndex: Int,
) : Serializable {
    class V0(
        val privateIdObjectDataEncrypted: String,
        val accountName: String,
        val encryptedAccountData: String,
        val accountAddress: String,
        identityProvider: IdentityProvider,
        idObjectRequest: RawJson,
        identityName: String,
        identityIndex: Int
    ) : IdentityCreationData(
        identityProvider,
        idObjectRequest,
        identityName,
        identityIndex,
    )

    class V1(
        identityProvider: IdentityProvider,
        idObjectRequest: RawJson,
        identityName: String,
        identityIndex: Int,
    ) : IdentityCreationData(
        identityProvider,
        idObjectRequest,
        identityName,
        identityIndex,
    )
}
