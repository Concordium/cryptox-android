package com.concordium.wallet.data.walletconnect

import java.io.Serializable

sealed interface Schema : Serializable {
    /**
     * The version is present for "module" type schema.
     */
    val version: Int?

    data class ValueSchema(
        val type: String?,
        val value: String?,
        override val version: Int?,
    ) : Schema

    /**
     * In some cases the buffer object in Java Script has been serialized directly, instead of first
     * converting the buffer object to a hex string and thereafter converting the string to a
     * byte array.
     */
    data class BrokenSchema(
        val type: String?,
        val value: BrokenValue,
        override val version: Int?,
    ) : Schema {
        data class BrokenValue(val type: String?, val data: List<Int>): Serializable
    }
}
