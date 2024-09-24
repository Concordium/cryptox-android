package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.math.BigInteger

data class AccountCooldown(
    val timestamp: Long,
    val amount: BigInteger,
    val status: Status,
): Serializable {
    enum class Status {
        /**
         * The stake is no longer effective for the current payday,
         * and will become available at the specified time.
         */
        @SerializedName("cooldown")
        COOLDOWN,

        /**
         * The stake may still be effective for the current payday,
         * and will begin cooldown at the next payday,
         * and is expected to become available at the specified time.
         */
        @SerializedName("precooldown")
        PRE_COOLDOWN,

        /**
         * The stake may still be effective for the current payday,
         * and will enter precooldown at the next snapshot epoch (i.e. one epoch before the payday),
         * and is expected to become available at the specified time.
         */
        @SerializedName("preprecooldown")
        PRE_PRE_COOLDOWN,
        ;
    }
}
