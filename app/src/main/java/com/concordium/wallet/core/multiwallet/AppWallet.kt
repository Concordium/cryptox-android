package com.concordium.wallet.core.multiwallet

import com.concordium.wallet.core.migration.TwoWalletsMigration
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

    /**
     * Whether this is the first set up wallet in the app,
     * either created or migrated by the [TwoWalletsMigration].
     */
    val isPrimary: Boolean
        get() = id == PRIMARY_WALLET_ID

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppWallet) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "AppWallet(id='$id', type=$type)"
    }

    enum class Type {
        FILE,
        SEED,
        ;
    }

    companion object {
        const val PRIMARY_WALLET_ID = ""

        fun primary(
            type: Type,
        ) = AppWallet(
            id = PRIMARY_WALLET_ID,
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
