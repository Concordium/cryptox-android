package com.concordium.wallet.data.model

import java.io.Serializable

class PLTModuleState(
    val name: String?,
    val metadata: Metadata?,
    val allowList: Boolean?,
    val paused: Boolean,
) : Serializable {

    class Metadata(
        val url: String,
        val checksumSha256: String?,
    )
}
