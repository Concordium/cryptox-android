package com.concordium.wallet.core.authentication

import com.concordium.wallet.App
import com.concordium.wallet.core.security.EncryptionException
import com.concordium.wallet.core.security.EncryptionHelper
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.core.security.KeystoreHelper
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException

/**
 * Handles app-wide password auth and encryption.
 *
 * @param slot identifier of a slot to read/write the auth data into.
 * Use AuthenticationManagers with different slots to safely update the auth
 * in the same way the A/B flashing works in smartphones.
 */
class AuthenticationManager(
    val slot: String,
) {
    private val authPreferences = AuthPreferences(App.appContext)

    // region Biometrics

    fun initBiometricAuth(
        password: String,
        cipher: Cipher,
    ): Boolean {
        try {
            authPreferences.setEncryptedPassword(
                slot,
                EncryptedData(
                    ciphertext = cipher.doFinal(password.toByteArray()),
                    iv = cipher.iv,
                    transformation = cipher.algorithm,
                )
            )
            authPreferences.setUseBiometrics(slot, true)
            return true
        } catch (e: java.lang.Exception) {
            when (e) {
                is BadPaddingException,
                is IllegalBlockSizeException -> {
                    Log.e("Failed to encrypt the data with the generated key. ${e.message}")
                    return false
                }

                else -> throw e
            }
        }
    }

    fun generateBiometricsSecretKey(): Boolean {
        return try {
            KeystoreHelper().generateSecretKey(slot)
            true
        } catch (e: KeystoreEncryptionException) {
            false
        }
    }

    /**
     * Can throw KeystoreEncryptionException or return null in case of KeyPermanentlyInvalidatedException
     */
    fun getBiometricsCipherForEncryption(): Cipher? {
        return KeystoreHelper().initCipherForEncryption(slot)
    }

    /**
     * Can throw KeystoreEncryptionException or return null in case of KeyPermanentlyInvalidatedException
     */
    fun getBiometricsCipherForDecryption(): Cipher? {
        try {
            val cipher = KeystoreHelper().initCipherForDecryption(
                keyName = slot,
                initVector = authPreferences.getEncryptedPassword(slot).decodeIv(),
            )

            if (cipher == null) {
                authPreferences.setUseBiometrics(slot, false)
            }

            return cipher
        } catch (e: KeystoreEncryptionException) {
            authPreferences.setUseBiometrics(slot, false)
            throw e
        }
    }

    suspend fun decryptPasswordWithBiometricsCipher(
        cipher: Cipher,
    ): String? = withContext(Dispatchers.Default) {
        runCatching {
            val encryptedPassword = authPreferences.getEncryptedPassword(slot)
            cipher.doFinal(encryptedPassword.decodeCiphertext())
        }
            .getOrNull()
            ?.let(::String)
    }

    //endregion

    /**
     * Initializes the password auth generating a new encryption master key.
     * Use this method to init the auth for the first time.
     */
    @Throws(EncryptionException::class)
    suspend fun initPasswordAuth(password: String) =
        initPasswordAuth(
            password = password,
            masterKey = EncryptionHelper.generateKey(),
        )

    /**
     * Initializes the password auth reusing the existing master key.
     * Use this method in combination with the current master key
     * to change the existing password.
     */
    @Throws(EncryptionException::class)
    suspend fun initPasswordAuth(
        password: String,
        masterKey: ByteArray,
    ) {
        val passwordKeySalt = EncryptionHelper.generatePasswordKeySalt()
        val passwordKey = EncryptionHelper.generatePasswordKey(
            password = password.toCharArray(),
            salt = passwordKeySalt,
        )
        val encryptedMasterKey: EncryptedData = EncryptionHelper.encrypt(
            key = passwordKey,
            data = masterKey,
        )
        authPreferences.setPasswordKeySalt(
            slot,
            passwordKeySalt
        )
        authPreferences.setEncryptedMasterKey(
            slot,
            encryptedMasterKey
        )
    }

    @Throws(EncryptionException::class)
    suspend fun getMasterKey(
        password: String,
    ): ByteArray {
        val encryptedMasterKey = authPreferences.getEncryptedMasterKey(slot)
        val passwordKey = EncryptionHelper.generatePasswordKey(
            password = password.toCharArray(),
            salt = authPreferences.getPasswordKeySalt(slot),
        )
        return EncryptionHelper.decrypt(
            key = passwordKey,
            encryptedData = encryptedMasterKey
        )
    }

    suspend fun checkPassword(password: String): Boolean =
        runCatching { getMasterKey(password) }.getOrNull() != null

    suspend fun encrypt(
        password: String,
        data: ByteArray,
    ): EncryptedData? =
        runCatching {
            encrypt(
                masterKey = getMasterKey(password),
                data = data,
            )
        }.getOrNull()

    suspend fun encrypt(
        masterKey: ByteArray,
        data: ByteArray,
    ): EncryptedData? =
        runCatching {
            EncryptionHelper.encrypt(
                key = masterKey,
                data = data,
            )
        }.getOrNull()

    suspend fun decrypt(
        password: String,
        encryptedData: EncryptedData,
    ): ByteArray? =
        runCatching {
            decrypt(
                masterKey = getMasterKey(password),
                encryptedData = encryptedData,
            )
        }.getOrNull()

    suspend fun decrypt(
        masterKey: ByteArray,
        encryptedData: EncryptedData,
    ): ByteArray? =
        runCatching {
            EncryptionHelper.decrypt(
                key = masterKey,
                encryptedData = encryptedData,
            )
        }.getOrNull()

    fun usePasscode(): Boolean {
        return authPreferences.getUsePasscode(slot)
    }

    fun useBiometrics(): Boolean {
        return authPreferences.getUseBiometrics(slot)
    }

    fun setUsePassCode(passcodeUsed: Boolean) {
        authPreferences.setUsePasscode(slot, passcodeUsed)
    }
}
