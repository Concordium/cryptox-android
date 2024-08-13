package com.concordium.wallet.data.backend.notifications

import retrofit2.http.PUT

interface NotificationsBackend {
    @PUT("v1/device/{token}/subscription")
    fun updateSubscription(request: UpdateSubscriptionRequest): Any
}
