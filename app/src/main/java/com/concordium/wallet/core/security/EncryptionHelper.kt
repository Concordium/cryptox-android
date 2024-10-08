package com.concordium.wallet.core.security

import android.security.keystore.KeyProperties
import android.util.Base64
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.UnsupportedEncodingException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encryption functions. Beware that these are used for both authentication and export.
 */
object EncryptionHelper {

    private val t = this.javaClass.simpleName

    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val KDF_ITERATION_COUNT = 10000
    private const val KDF_SALT_SIZE_BYTES = 32

    private const val ENCRYPTION_KEY_SIZE_BITS = 256
    private const val ENCRYPTION_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val NEW_CIPHER_TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/NoPadding"
    private const val LEGACY_CIPHER_TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/PKCS7Padding"

    /**
     * @exception EncryptionException
     */
    fun createEncryptionData(): Pair<ByteArray, ByteArray> {
        val saltLength = ENCRYPTION_KEY_SIZE_BITS / 8 // same size as key output
        val random = SecureRandom()
        val salt = ByteArray(saltLength)
        random.nextBytes(salt)

        try {
            val cipher = Cipher.getInstance(LEGACY_CIPHER_TRANSFORMATION)
            val iv = ByteArray(cipher.blockSize)
            random.nextBytes(iv)

            return Pair(salt, iv)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchPaddingException -> {
                    Log.d("$t: Failed creating encryption data", e)
                    throw EncryptionException(e)
                }

                else -> throw e
            }
        }
    }

    /**
     * @exception EncryptionException
     */
    @Throws(EncryptionException::class)
    fun generateKey(
        password: String,
        salt: ByteArray,
        iterationCount: Int = KDF_ITERATION_COUNT
    ): SecretKey {
        Log.i("$t: generateKey. Params --> password: $password, salt: $salt")
        val keyBytes = generateKeyAsByteArray(password, salt, iterationCount)
        val key: SecretKey = SecretKeySpec(keyBytes, "AES")
        return key
    }

    /**
     * @exception EncryptionException
     */
    private fun generateKeyAsByteArray(
        password: String,
        salt: ByteArray,
        iterationCount: Int = KDF_ITERATION_COUNT
    ): ByteArray {
        try {
            val keySpec: KeySpec =
                PBEKeySpec(
                    password.toCharArray(), salt,
                    iterationCount,
                    ENCRYPTION_KEY_SIZE_BITS
                )
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = keyFactory.generateSecret(keySpec).encoded
            return keyBytes
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is InvalidKeySpecException -> {
                    Log.d("Failed to create key", e)
                    throw EncryptionException(e)
                }

                else -> throw e
            }
        }
    }

    /**
     * @exception EncryptionException
     */
    fun encrypt(
        key: SecretKey,
        iv: ByteArray,
        toBeEncrypted: String,
        base64EncodeFlags: Int = Base64.DEFAULT
    ): String {
        try {
            val toBeEncryptedByteArray = toBeEncrypted.toByteArray(charset("UTF-8"))
            val cipher = Cipher.getInstance(LEGACY_CIPHER_TRANSFORMATION)
            val ivParams = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams)
            val cipherText = cipher.doFinal(toBeEncryptedByteArray)
            val encodedEncrypted = Base64.encodeToString(cipherText, base64EncodeFlags)
            Log.d("Encrypted text: $encodedEncrypted")
            return encodedEncrypted
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

    /**
     * @exception EncryptionException
     */
    fun decrypt(
        key: SecretKey,
        iv: ByteArray,
        toBeDecrypted: ByteArray
    ): String {
        try {
            val cipher = Cipher.getInstance(LEGACY_CIPHER_TRANSFORMATION)
            val ivParams = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, ivParams)
            val plainText = cipher.doFinal(toBeDecrypted)
            val decryptedString = String(plainText, charset("UTF-8"))
            Log.d("Decrypted text: $decryptedString")
            return decryptedString
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

    /**
     * @exception EncryptionException
     */
    fun encrypt(
        password: String,
        salt: ByteArray,
        iv: ByteArray,
        toBeEncrypted: String,
        base64EncodeFlags: Int = Base64.DEFAULT
    ): String {
        val key = generateKey(password, salt)
        return encrypt(key, iv, toBeEncrypted, base64EncodeFlags)
    }

    /**
     * @exception EncryptionException
     */
    fun decrypt(
        password: String,
        salt: ByteArray,
        iv: ByteArray,
        toBeDecrypted: ByteArray
    ): String {
        val key = generateKey(password, salt)
        return decrypt(key, iv, toBeDecrypted)
    }

    /**
     * Encrypts given [data] with the given [key] using the best suitable cipher.
     *
     * @return [EncryptedData] with a unique IV.
     */
    @Throws(EncryptionException::class)
    suspend fun encrypt(
        key: ByteArray,
        data: ByteArray,
    ): EncryptedData = withContext(Dispatchers.Default) {
        try {
            val cipher = Cipher.getInstance(NEW_CIPHER_TRANSFORMATION)
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
            val cipherText = encryptedData.decodeCipherText()
            val iv = encryptedData.decodeIv()
            val cipher = Cipher.getInstance(encryptedData.transformation)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, cipher.algorithm),
                IvParameterSpec(iv)
            )
            cipher.doFinal(cipherText)
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
    suspend fun generateMasterKey(): ByteArray = withContext(Dispatchers.Default) {
        try {
            val generator = KeyGenerator.getInstance(ENCRYPTION_KEY_ALGORITHM)
            generator.init(ENCRYPTION_KEY_SIZE_BITS)
            generator.generateKey().encoded
        } catch (e: Exception) {
            Log.d("Failed to generate master key", e)
            throw EncryptionException(e)
        }
    }

    @Throws(EncryptionException::class)
    suspend fun generatePasswordKeySalt(): ByteArray = withContext(Dispatchers.Default) {
        try {
            SecureRandom().generateSeed(KDF_SALT_SIZE_BYTES)
        } catch (e: Exception){
            Log.d("Failed to generate password key salt", e)
            throw EncryptionException(e)
        }
    }

    @Throws(EncryptionException::class)
    suspend fun generatePasswordKey(
        password: CharArray,
        salt: ByteArray,
    ): ByteArray = withContext(Dispatchers.Default) {
        try {
            val keySpec: KeySpec = PBEKeySpec(
                password,
                salt,
                KDF_ITERATION_COUNT,
                ENCRYPTION_KEY_SIZE_BITS,
            )
            val keyFactory = SecretKeyFactory.getInstance(KDF)
            keyFactory.generateSecret(keySpec).encoded
        } catch (e: Exception) {
            Log.d("Failed to generate password key", e)
            throw EncryptionException(e)
        }
    }
}
