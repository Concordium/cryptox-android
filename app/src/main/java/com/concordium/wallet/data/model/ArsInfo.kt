package com.concordium.wallet.data.model

import com.concordium.sdk.responses.blocksummary.updates.queues.AnonymityRevokerInfo
import com.concordium.sdk.serializing.JsonMapper
import com.concordium.wallet.App
import java.io.Serializable

data class ArsInfo(
    val arIdentity: Int,
    val arPublicKey: String,
    val arDescription: ArDescription,
) : Serializable {

    fun toSdkAnonymityRevokerInfo(): AnonymityRevokerInfo =
        JsonMapper.INSTANCE.readValue(
            App.appCore.gson.toJson(this),
            AnonymityRevokerInfo::class.java
        )
}
