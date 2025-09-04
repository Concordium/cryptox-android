package com.concordium.wallet.data.model

import java.io.Serializable

/**
 * See [CIS-2 Token metadata JSON](https://proposals.concordium.software/CIS/cis-2.html#token-metadata-json)
 */
class ContractTokenMetadata(
    val decimals: Int?,
    val description: String? = null,
    val name: String? = null,
    val symbol: String? = null,
    val thumbnail: UrlHolder? = null,
    val unique: Boolean? = null,
    val display: UrlHolder? = null,
) : Serializable

/**
 * See [CIS-7 Token metadata format](https://proposals.concordium.com/CIS/cis-7.html#token-metadata-format)
 */
class ProtocolLevelTokenMetadata(
    val description: String? = null,
    val thumbnail: UrlHolder? = null,
): Serializable

data class UrlHolder(
    val url: String?,
) : Serializable
