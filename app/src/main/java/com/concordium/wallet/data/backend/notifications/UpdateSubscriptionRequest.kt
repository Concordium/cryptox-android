package com.concordium.wallet.data.backend.notifications

import com.concordium.wallet.data.model.NotificationsTopic
import com.google.gson.annotations.SerializedName

class UpdateSubscriptionRequest(
    @SerializedName("device_token")
    val fcmToken: String,
    @SerializedName("preferences")
    val preferences: Set<NotificationsTopic> = emptySet(),
    @SerializedName("accounts")
    val accounts: Set<String> = emptySet(),
)
