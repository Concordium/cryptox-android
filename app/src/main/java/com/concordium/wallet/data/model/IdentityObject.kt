package com.concordium.wallet.data.model

import com.concordium.sdk.crypto.wallet.identityobject.IdentityObject
import com.concordium.sdk.serializing.JsonMapper
import com.concordium.wallet.App
import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.google.gson.annotations.JsonAdapter
import java.io.Serializable

data class IdentityObject(
    val attributeList: AttributeList,
    val preIdentityObject: PreIdentityObject,
    @JsonAdapter(RawJsonTypeAdapter::class)
    val signature: RawJson,
) : Serializable {

    @Transient
    private var memoizedSdkIdentityObject: IdentityObject? = null
    fun toSdkIdentityObject(): IdentityObject =
        memoizedSdkIdentityObject
            ?: JsonMapper
                .INSTANCE
                .readValue(
                    App.appCore.gson.toJson(this),
                    IdentityObject::class.java
                )
                .also { memoizedSdkIdentityObject = it }
}
