package com.concordium.wallet.data.backend.airdrop

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface AirDropBackend {

    @POST // ("/v2/airdrop/register-wallet")
    suspend fun getTokens(@Url url: String, @Body request: RegistrationRequest): RegistrationResponse
}