package com.concordium.wallet.ui.onramp

class CcdOnrampSite(
    val name: String,
    val url: String,
    val logoUrl: String,
    val type: Type,
) {
    enum class Type {
        PAYMENT_GATEWAY,
        CEX,
        ;
    }
}
