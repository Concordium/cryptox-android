package com.concordium.wallet.data.util

import android.security.keystore.KeyProperties
import android.util.Base64
import com.concordium.wallet.core.security.EncryptionException
import com.concordium.wallet.core.security.EncryptionHelper
import com.concordium.wallet.data.export.EncryptedExportData
import com.concordium.wallet.data.export.ExportEncryptionMetaData
import com.concordium.wallet.data.model.EncryptedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExportEncryptionHelper {

    /**
     * @exception EncryptionException
     */
    @Throws(EncryptionException::class)
    suspend fun encryptExportData(
        password: String,
        toBeEncrypted: String,
    ): EncryptedExportData = withContext(Dispatchers.Default) {
        // This method must remain backward-compatible with the Concordium file export,
        // therefore it uses own encryption params and Base64 encoding.

        val salt = EncryptionHelper.generatePasswordKeySalt()
        val key = EncryptionHelper.generatePasswordKey(
            password = password.toCharArray(),
            salt = salt,
            sizeBits = ENCRYPTION_KEY_SIZE_BITS,
            kdf = KDF,
            kdfIterationCount = KDF_ITERATION_COUNT,
        )
        val encryptedData = EncryptionHelper.encrypt(
            key = key,
            data = toBeEncrypted.toByteArray(),
            cipherTransformation = CIPHER_TRANSFORMATION,
        )

        EncryptedExportData(
            metadata = ExportEncryptionMetaData(
                encryptionMethod = ENCRYPTION_METHOD,
                keyDerivationMethod = KDF,
                iterations = KDF_ITERATION_COUNT,
                salt = Base64.encodeToString(salt, Base64.NO_WRAP),
                initializationVector = Base64.encodeToString(
                    encryptedData.decodeIv(),
                    Base64.NO_WRAP
                ),
            ),
            cipherText = Base64.encodeToString(encryptedData.decodeCiphertext(), Base64.NO_WRAP),
        )
    }

    @Throws(EncryptionException::class)
    suspend fun decryptExportData(
        password: String,
        encryptedExportData: EncryptedExportData,
    ): String = withContext(Dispatchers.Default) {
        val key = EncryptionHelper.generatePasswordKey(
            password = password.toCharArray(),
            salt = Base64.decode(encryptedExportData.metadata.salt, Base64.DEFAULT),
            kdf = encryptedExportData.metadata.keyDerivationMethod,
            kdfIterationCount = encryptedExportData.metadata.iterations,
            sizeBits = ENCRYPTION_KEY_SIZE_BITS,
        )
        val wrappedEncryptedExportData = EncryptedData(
            ciphertext = Base64.decode(encryptedExportData.cipherText, Base64.DEFAULT),
            iv = Base64.decode(encryptedExportData.metadata.initializationVector, Base64.DEFAULT),
            transformation = CIPHER_TRANSFORMATION,
        )

        EncryptionHelper.decrypt(
            key = key,
            encryptedData = wrappedEncryptedExportData,
        ).let(::String)
    }

    private const val KDF_ITERATION_COUNT = 100000
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val ENCRYPTION_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val ENCRYPTION_KEY_SIZE_BITS = 256
    private const val ENCRYPTION_METHOD = "$ENCRYPTION_KEY_ALGORITHM-$ENCRYPTION_KEY_SIZE_BITS"
    private const val CIPHER_TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/PKCS7Padding"
}
