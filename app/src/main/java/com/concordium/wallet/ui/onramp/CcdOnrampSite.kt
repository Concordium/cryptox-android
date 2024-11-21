package com.concordium.wallet.ui.onramp

import java.io.Serializable

data class CcdOnrampSite(
    val name: String,
    val url: String,
    val logoUrl: String,
    val type: Type,
    val acceptsCreditCard: Boolean = false,
): Serializable {
    enum class Type {
        PAYMENT_GATEWAY,
        CEX,
        DEX
        ;
    }
}
