package com.concordium.wallet.ui.onramp

import androidx.annotation.StringRes
import com.concordium.wallet.R

sealed interface CcdOnrampListItem {
    object Header : CcdOnrampListItem

    class Section(
        @StringRes
        val nameRes: Int,
    ) : CcdOnrampListItem {
        constructor(siteType: CcdOnrampSite.Type) : this(
            nameRes = when (siteType) {
                CcdOnrampSite.Type.PAYMENT_GATEWAY ->
                    R.string.ccd_onramp_site_type_payment_gateway

                CcdOnrampSite.Type.CEX ->
                    R.string.ccd_onramp_site_type_cex
            }
        )
    }

    class Site(
        val name: String,
        val logoUrl: String,
        val isDividerVisible: Boolean,
        val isCreditCardVisible: Boolean,
        val source: CcdOnrampSite?,
    ) : CcdOnrampListItem {
        constructor(
            source: CcdOnrampSite,
            isDividerVisible: Boolean,
        ) : this(
            name = source.name,
            logoUrl = source.logoUrl,
            isCreditCardVisible = source.acceptsCreditCard,
            isDividerVisible = isDividerVisible,
            source = source,
        )
    }

    object NoneAvailable : CcdOnrampListItem

    object Disclaimer : CcdOnrampListItem
}
