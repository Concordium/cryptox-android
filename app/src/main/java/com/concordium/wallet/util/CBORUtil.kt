package com.concordium.wallet.util

import com.google.iot.cbor.CborObject
import java.lang.Exception

class CBORUtil {
    companion object {
        const val MAX_BYTES = 256

        /**
         * CBOR encode to byte array
         */
        fun encodeCBOR(txt: String): ByteArray {
            val cborMap = CborObject.createFromJavaObject(txt)
            return cborMap.toCborByteArray()
        }

        /**
         * Hex decode and CBOR decode, in one go
         */
        fun decodeHexAndCBOR(txt: String): String {
            val byteArray = txt.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            if (byteArray.isEmpty()) {
                return ""
            }
            return try {
                val cborObj = CborObject.createFromCborByteArray(byteArray)
                cborObj.toJavaObject() as String
            } catch (e: Exception) {
                Log.e("Error parsing CBOR: " + e)
                txt
            }
        }
    }
}
