package com.concordium.wallet.data.backend.notifications

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

interface NotificationsBackend {
    @PUT("v1/subscription")
    suspend fun updateSubscription(@Body request: UpdateSubscriptionRequest): Result<Boolean>

    @POST("v1/unsubscribe")
    suspend fun unsubscribe(@Body request: UpdateSubscriptionRequest): Result<Boolean>
}
