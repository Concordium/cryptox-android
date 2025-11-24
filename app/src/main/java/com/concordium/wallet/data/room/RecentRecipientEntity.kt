package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListItem
import com.concordium.wallet.ui.recipient.recipientlist.RecipientType
import java.io.Serializable

@Entity(tableName = "recent_recipient_table")
data class RecentRecipientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "address")
    val address: String,
    @ColumnInfo(name = "added_at", defaultValue = "0")
    val addedAt: Long = 0L
) : Serializable {

    fun toRecipientItem() = RecipientListItem.RecipientItem(
        name = name,
        address = address,
        addedAt = addedAt,
        recipientType = RecipientType.RECENT
    )
}
