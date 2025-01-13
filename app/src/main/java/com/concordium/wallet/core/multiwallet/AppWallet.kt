package com.concordium.wallet.core.multiwallet

import com.concordium.wallet.data.room.app.AppWalletEntity
import java.util.Date

class AppWallet
private constructor(
    val id: String,
    val type: Type,
    val createdAt: Date,
) {
    constructor(entity: AppWalletEntity) : this(
        id = entity.id,
        type = Type.valueOf(entity.type),
        createdAt = Date(entity.createdAt),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppWallet) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    enum class Type {
        FILE,
        SEED,
        ;
    }

    companion object {
        fun primary(
            type: Type,
        ) = AppWallet(
            id = "",
            type = type,
            createdAt = Date(0),
        )

        fun extra(
            type: Type,
        ): AppWallet {
            val now = Date()

            return AppWallet(
                id = now.time.toString(),
                type = type,
                createdAt = now,
            )
        }
    }
}
