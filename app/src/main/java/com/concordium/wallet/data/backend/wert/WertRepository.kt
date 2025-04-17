package com.concordium.wallet.data.backend.wert

import com.concordium.wallet.App

class WertRepository {

    private val backend = App.appCore.getWertBackend()

    suspend fun getWertSessionDetails(walletAddress: String) =
        backend.getSessionDetails(WertSessionRequest(walletAddress = walletAddress))
}