package com.concordium.wallet.data.model

import android.util.Base64
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * An encrypted data container.
 */
class EncryptedData(
    /**
     * Base64-encoded ciphertext.
     *
     * @see decodeCiphertext
     */
    @SerializedName("ct")
    val ciphertext: String,

    /**
     * Java-style cipher transformation, e.g. "AES/CBC/PKCS7Padding".
     */
    @SerializedName("t")
    val transformation: String,

    /**
     * Base64-encoded cipher initialization vector.
     *
     * @see decodeIv
     */
    @SerializedName("iv")
    val iv: String,
): Serializable {

    /**
     * @param ciphertext raw ciphertext
     * @param transformation Java-style cipher transformation, e.g. "AES/CBC/PKCS7Padding"
     * @param iv raw cipher initialization vector
     */
    constructor(
        ciphertext: ByteArray,
        transformation: String,
        iv: ByteArray,
    ) : this(
        ciphertext = Base64.encodeToString(ciphertext, BASE64_FLAGS),
        transformation = transformation,
        iv = Base64.encodeToString(iv, BASE64_FLAGS)
    )

    fun decodeCiphertext(): ByteArray =
        Base64.decode(ciphertext, BASE64_FLAGS)

    fun decodeIv(): ByteArray =
        Base64.decode(iv, BASE64_FLAGS)

    override fun toString(): String {
        return "EncryptedData(ciphertext='$ciphertext', transformation='$transformation', iv='$iv')"
    }

    private companion object {
        private const val BASE64_FLAGS = Base64.NO_WRAP or Base64.NO_PADDING
    }
}
