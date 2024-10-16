package com.concordium.wallet.core.multiwallet

class WalletInfo
private constructor(
    val id: String,
    val type: Type,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WalletInfo) return false

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
        ) = WalletInfo(
            id = "",
            type = type,
        )

        fun extra(
            type: Type,
        ) = WalletInfo(
            id = System.currentTimeMillis().toString(),
            type = type,
        )
    }
}
