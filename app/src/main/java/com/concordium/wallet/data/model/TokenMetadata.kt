package com.concordium.wallet.data.model

import java.io.Serializable

/**
 * See [CIS-2 Token metadata JSON](https://proposals.concordium.software/CIS/cis-2.html#token-metadata-json)
 */
data class TokenMetadata(
    val decimals: Int?,
    val description: String?,
    val name: String?,
    val symbol: String?,
    val thumbnail: UrlHolder?,
    val unique: Boolean?,
    var display: UrlHolder?,
) : Serializable

data class UrlHolder(
    val url: String?,
) : Serializable
