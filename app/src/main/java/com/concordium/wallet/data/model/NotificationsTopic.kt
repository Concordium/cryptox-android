package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName

enum class NotificationsTopic {
    @SerializedName("cis2-tx")
    CIS2_TRANSACTIONS,

    @SerializedName("ccd-tx")
    CCD_TRANSACTIONS,
    ;
}
