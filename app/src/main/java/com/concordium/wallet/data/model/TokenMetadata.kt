package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

/**
 * See [CIS-2 Token metadata JSON](https://proposals.concordium.software/CIS/cis-2.html#token-metadata-json)
 */
data class TokenMetadata(
    val decimals: Int?,
    val description: String? = null,
    val name: String? = null,
    val symbol: String? = null,
    val thumbnail: UrlHolder? = null,
    val unique: Boolean? = null,
    var display: UrlHolder? = null,
    val totalSupply: BigInteger? = null
) : Serializable

data class UrlHolder(
    val url: String?,
) : Serializable
