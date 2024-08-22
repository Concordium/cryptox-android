package com.concordium.wallet.data.backend.notifications

import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificationsBackend {
    @PUT("v1/device/{token}/subscription")
    suspend fun updateSubscription(
        @Path("token")
        fcmToken: String,
        @Body
        request: UpdateSubscriptionRequest,
    ): Result<Boolean>
}
