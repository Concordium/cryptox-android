package com.concordium.wallet.data.backend.wert

import com.concordium.wallet.App

class WertRepository {

    private val backend = App.appCore.getWertBackend()

    suspend fun getWertSessionDetails(
        walletAddress: String,
        success: (WertSessionResponse) -> Unit,
        failure: ((Throwable) -> Unit)?
    ) {
        try {
            val response =
                backend.getSessionDetails(WertSessionRequest(walletAddress = walletAddress))

            if (response.isSuccessful) {
                response.body()?.let { success(it) }
            } else {
                failure?.invoke(Throwable("Error: ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            failure?.invoke(e)
        }
    }
}