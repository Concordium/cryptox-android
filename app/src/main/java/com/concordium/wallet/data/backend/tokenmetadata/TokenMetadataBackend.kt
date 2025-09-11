package com.concordium.wallet.data.backend.tokenmetadata

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

interface TokenMetadataBackend {
    @GET
    @Headers("Accept: application/json")
    suspend fun getMetadataJson(
        @Url
        url: String,
    ): String
}
