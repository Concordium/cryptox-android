package com.concordium.wallet.data.model

import com.concordium.sdk.responses.blocksummary.updates.queues.IdentityProviderInfo
import com.concordium.sdk.serializing.JsonMapper
import com.concordium.wallet.App
import java.io.Serializable

data class IdentityProviderInfo(
    val ipIdentity: Int,
    val ipDescription: IdentityProviderDescription,
    val ipVerifyKey: String,
    val ipCdiVerifyKey: String,
) : Serializable {

    fun toSdkIdentityProviderInfo(): IdentityProviderInfo =
        JsonMapper.INSTANCE.readValue(
            App.appCore.gson.toJson(this),
            IdentityProviderInfo::class.java
        )
}
