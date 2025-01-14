package com.concordium.wallet.core.security

import android.security.keystore.KeyProperties
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.UnsupportedEncodingException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encryption and key derivation functions.
 */
object EncryptionHelper {
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val KDF_ITERATION_COUNT = 10000

    private const val ENCRYPTION_KEY_SIZE_BITS = 256
    private const val ENCRYPTION_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val ENCRYPTION_CIPHER_TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/NoPadding"

    /**
     * Encrypts given [data] with the given [key] using the best suitable cipher.
     *
     * @return [EncryptedData] with a unique IV.
     */
    @Throws(EncryptionException::class)
    suspend fun encrypt(
        key: ByteArray,
        data: ByteArray,
        cipherTransformation: String = ENCRYPTION_CIPHER_TRANSFORMATION,
    ): EncryptedData = withContext(Dispatchers.Default) {
        try {
            val cipher = Cipher.getInstance(cipherTransformation)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, cipher.algorithm))
            EncryptedData(
                ciphertext = cipher.doFinal(data),
                transformation = cipher.algorithm,
                iv = cipher.iv,
            )
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchPaddingException,
                is InvalidAlgorithmParameterException,
                is InvalidKeyException,
                is UnsupportedEncodingException,
                is BadPaddingException,
                is IllegalBlockSizeException -> {
                    Log.d("Failed to encrypt data", e)
                    throw EncryptionException(e)
                }

                else -> throw e
            }
        }
    }

    @Throws(EncryptionException::class)
    suspend fun decrypt(
        key: ByteArray,
        encryptedData: EncryptedData,
    ): ByteArray = withContext(Dispatchers.Default) {
        try {
            val cipher = Cipher.getInstance(encryptedData.transformation)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, cipher.algorithm),
                IvParameterSpec(encryptedData.decodeIv())
            )
            cipher.doFinal(encryptedData.decodeCiphertext())
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchPaddingException,
                is InvalidAlgorithmParameterException,
                is InvalidKeyException,
                is UnsupportedEncodingException,
                is BadPaddingException,
                is IllegalBlockSizeException -> {
                    Log.d("Failed to decrypt data", e)
                    throw EncryptionException(e)
                }

                else -> throw e
            }
        }
    }

    @Throws(EncryptionException::class)
    suspend fun generateKey(): ByteArray = withContext(Dispatchers.Default) {
        try {
            val generator = KeyGenerator.getInstance(ENCRYPTION_KEY_ALGORITHM)
            generator.init(ENCRYPTION_KEY_SIZE_BITS)
            generator.generateKey().encoded
        } catch (e: Exception) {
            Log.d("Failed to generate a key", e)
            throw EncryptionException(e)
        }
    }

    @Throws(EncryptionException::class)
    suspend fun generatePasswordKeySalt(): ByteArray = withContext(Dispatchers.Default) {
        try {
            SecureRandom().generateSeed(ENCRYPTION_KEY_SIZE_BITS / 8)
        } catch (e: Exception) {
            Log.d("Failed to generate password key salt", e)
            throw EncryptionException(e)
        }
    }

    @Throws(EncryptionException::class)
    suspend fun generatePasswordKey(
        password: CharArray,
        salt: ByteArray,
        sizeBits: Int = ENCRYPTION_KEY_SIZE_BITS,
        kdf: String = KDF,
        kdfIterationCount: Int = KDF_ITERATION_COUNT,
    ): ByteArray = withContext(Dispatchers.Default) {
        try {
            SecretKeyFactory.getInstance(kdf)
                .generateSecret(
                    PBEKeySpec(
                        password,
                        salt,
                        kdfIterationCount,
                        sizeBits,
                    )
                )
                .encoded
        } catch (e: Exception) {
            Log.d("Failed to generate password key", e)
            throw EncryptionException(e)
        }
    }
}
