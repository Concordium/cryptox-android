package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListItem
import com.concordium.wallet.ui.recipient.recipientlist.RecipientType
import java.io.Serializable

@Entity(tableName = "recent_recipient_table")
data class RecentRecipientEntity(
    @PrimaryKey
    @ColumnInfo(name = "address")
    val address: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
    @ColumnInfo(name = "name")
    val name: String? = null,
) : Serializable {

    fun toRecipientItem() = RecipientListItem.RecipientItem(
        name = name ?: Account.getDefaultName(address),
        address = address,
        addedAt = addedAt,
        recipientType = RecipientType.RECENT
    )
}
