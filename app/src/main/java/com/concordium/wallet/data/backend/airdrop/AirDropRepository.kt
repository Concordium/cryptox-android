package com.concordium.wallet.data.backend.airdrop

import com.concordium.wallet.App
import com.concordium.wallet.util.Log

class AirDropRepository {

    private val airdropBackend = App.appCore.getAirdropBackend()

    suspend fun doRegistration(registration: RegistrationRequest, apiUrl: String): RegistrationResponse? {
        return try {
            airdropBackend.getTokens(apiUrl, registration)
        } catch (ex: Exception) {
            Log.e("registration_failed", ex)
            null
        }
    }
}
