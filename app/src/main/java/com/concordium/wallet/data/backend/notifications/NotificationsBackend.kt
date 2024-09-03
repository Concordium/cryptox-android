package com.concordium.wallet.data.backend.notifications

import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificationsBackend {
    @PUT("v1/subscription")
    suspend fun updateSubscription(@Body request: UpdateSubscriptionRequest): Result<Boolean>
}
