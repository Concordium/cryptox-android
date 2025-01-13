package com.concordium.wallet.data.room.app

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.concordium.wallet.core.multiwallet.AppWallet

@Entity(
    tableName = "wallets",
    indices = [
        Index("created_at"),
        Index("is_active"),
    ]
)
class AppWalletEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
) {
    constructor(
        wallet: AppWallet,
        isActive: Boolean = false,
    ) : this(
        id = wallet.id,
        type = wallet.type.name,
        createdAt = wallet.createdAt.time,
        isActive = isActive,
    )
}
