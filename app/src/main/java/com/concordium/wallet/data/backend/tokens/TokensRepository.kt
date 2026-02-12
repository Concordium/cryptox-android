package com.concordium.wallet.data.backend.tokens

import com.concordium.wallet.App

class TokensRepository {

    private val backend = App.appCore.session.backends.tokens!!

    suspend fun getTokens(
        marketplaceDomain: String?,
        address: String,
        offset: Int = 0
    ): TokensResponse? {
        val request = TokensRequest(
            address = address,
            pageStart = offset
        )
        val tokens = try {
            backend.getTokens("$marketplaceDomain/api/v2/nft/get-by-address", request)
        } catch (e: Exception) {
            null
        }

        return tokens
    }
}
