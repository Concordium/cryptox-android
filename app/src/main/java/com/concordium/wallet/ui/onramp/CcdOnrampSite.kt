package com.concordium.wallet.ui.onramp

import androidx.annotation.DrawableRes
import java.io.Serializable

data class CcdOnrampSite(
    val name: String,
    val url: String,
    val logo: LogoSource,
    val type: Type,
    val acceptsCreditCard: Boolean = false,
) : Serializable {
    enum class Type {
        PAYMENT_GATEWAY,
        ;
    }
}

sealed class LogoSource : Serializable {
    data class Remote(val url: String) : LogoSource()
    data class Local(@param:DrawableRes val resId: Int) : LogoSource()
}
