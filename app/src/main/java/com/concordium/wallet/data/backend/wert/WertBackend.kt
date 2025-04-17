package com.concordium.wallet.data.backend.wert

import retrofit2.http.Body
import retrofit2.http.POST

interface WertBackend {
    @POST("external/hpp/create-session")
    suspend fun getSessionDetails(@Body request: WertSessionRequest): WertSessionResponse
}
