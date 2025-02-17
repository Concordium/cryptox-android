package com.concordium.wallet.data.model

data class CIS2Tokens(
    val count: Int,
    val from: Int,
    val limit: Int,
    val tokens: List<DataToken>
)

// Additional entity for correct id mapping
data class DataToken(
    val id: Int = 0,
    val token: String = ""
)

fun DataToken.toToken() = Token(
    uid = id.toString(),
    token = token
)
