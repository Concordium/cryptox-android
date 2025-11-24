package com.concordium.wallet.ui.recipient.recipientlist

import androidx.annotation.StringRes
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Recipient

sealed interface RecipientListItem {
    data class Category(
        @param:StringRes
        val nameRes: Int
    ) : RecipientListItem {
        constructor(recipientType: RecipientType) : this(
            nameRes = when (recipientType) {
                RecipientType.ADDRESS_BOOK -> R.string.recipient_list_default_title
                RecipientType.RECENT -> R.string.recipient_list_recents
            }
        )
    }

    data class RecipientItem(
        val name: String,
        val address: String,
        val recipientType: RecipientType,
        var addedAt: Long = 0
    ) : RecipientListItem {

        fun toRecipient() = Recipient(
            id = 0,
            name = name,
            address = address,
        )
    }
}