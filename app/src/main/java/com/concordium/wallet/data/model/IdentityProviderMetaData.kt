package com.concordium.wallet.data.model

import java.io.Serializable

data class IdentityProviderMetaData(
    val icon: String,
    val issuanceStart: String,
    val recoveryStart: String?,
    val display: String?,
) : Serializable
