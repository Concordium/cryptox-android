package com.concordium.wallet.data.model

import java.io.Serializable

data class GovernanceAccount(
    val address: String,
    val type: String
): Serializable
