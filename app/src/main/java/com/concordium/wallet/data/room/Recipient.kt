package com.concordium.wallet.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListItem
import com.concordium.wallet.ui.recipient.recipientlist.RecipientType
import java.io.Serializable


@Entity(tableName = "recipient_table")
data class Recipient(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    var name: String,
    var address: String,
) : Serializable {

    constructor(
        account: Account,
        id: Int = 0,
    ) : this(
        id = id,
        name = account.getAccountName(),
        address = account.address,
    )

    constructor(
        address: String,
    ) : this(
        id = 0,
        name = "",
        address = address,
    )

    fun toRecipientItem() = RecipientListItem.RecipientItem(
        id = id,
        name = name,
        address = address,
        recipientType = RecipientType.ADDRESS_BOOK
    )
}
