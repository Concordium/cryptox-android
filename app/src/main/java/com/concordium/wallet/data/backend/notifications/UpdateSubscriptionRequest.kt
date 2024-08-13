package com.concordium.wallet.data.backend.notifications

import com.concordium.wallet.data.model.NotificationsTopic
import com.google.gson.annotations.SerializedName

class UpdateSubscriptionRequest(
    @SerializedName("preferences")
    val preferences: Set<NotificationsTopic>,
    @SerializedName("accounts")
    val accounts: Set<String>
)
