package com.concordium.wallet.data.backend.tokens

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

// https://gitlab.com/tacans/spaceseven/services/protocol/proto/-/blob/master/api/v2/nft/api_get_by_address.proto
interface TokensBackend {

    @POST // ("/api/v2/nft/get-by-address")
    suspend fun getTokens(@Url url: String, @Body request: TokensRequest): TokensResponse
}