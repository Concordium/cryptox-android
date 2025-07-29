package com.concordium.wallet.data.model

import java.io.Serializable

data class PLTModuleState(
    val allowList: Boolean,
    val burnable: Boolean,
    val denyList: Boolean,
    val governanceAccount: GovernanceAccount,
    val metadata: PLTMetadata,
    val mintable: Boolean,
    val name: String,
    val paused: Boolean
) : Serializable

