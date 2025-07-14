package com.concordium.wallet.data.backend.devnet

import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.PLTInfo
import retrofit2.http.GET
import retrofit2.http.Path

interface DevnetBackend {
    @GET("/v0/plt/tokens")
    suspend fun pltTokens(): List<PLTInfo>

    @GET("/v0/plt/tokenInfo/{tokenId}")
    suspend fun getPLTTokenById(@Path("tokenId") tokenId: String): PLTInfo

    @GET("v2/accBalance/{accountAddress}")
    suspend fun accountBalanceSuspended(@Path("accountAddress") accountAddress: String): AccountBalance
}