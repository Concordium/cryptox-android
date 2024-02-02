package com.concordium.wallet.util

object HexUtil {

    fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    fun Long.toHexLE() = this.swap().toULong().toString(16).padStart(16, '0')
    fun Int.toHexLE() = this.toUByte().toString(16).padStart(2, '0')
    fun Int.toHexLEx32() = Integer.reverseBytes(this).toString(16).padStart(8, '0')

    private fun Long.swap(): Long {
        val b1 = this shr 0 and 0xff
        val b2 = this shr 8 and 0xff
        val b3 = this shr 16 and 0xff
        val b4 = this shr 24 and 0xff
        val b5 = this shr 32 and 0xff
        val b6 = this shr 40 and 0xff
        val b7 = this shr 48 and 0xff
        val b8 = this shr 56 and 0xff
        return b1 shl 56 or (b2 shl 48) or (b3 shl 40) or (b4 shl 32) or (
                b5 shl 24) or (b6 shl 16) or (b7 shl 8) or (b8 shl 0)
    }

    private fun Int.swap(): Int {
        val b1 = this shr 0 and 0xff
        val b2 = this shr 8 and 0xff
        val b3 = this shr 16 and 0xff
        val b4 = this shr 24 and 0xff
        val b5 = this shr 32 and 0xff
        val b6 = this shr 40 and 0xff
        val b7 = this shr 48 and 0xff
        val b8 = this shr 56 and 0xff
        return b1 shl 56 or (b2 shl 48) or (b3 shl 40) or (b4 shl 32) or (
                b5 shl 24) or (b6 shl 16) or (b7 shl 8) or (b8 shl 0)
    }
}