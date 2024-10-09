package com.concordium.wallet.core.authentication

import com.concordium.wallet.App
import com.concordium.wallet.core.security.EncryptionException
import com.concordium.wallet.core.security.EncryptionHelper
import com.concordium.wallet.core.security.KeystoreEncryptionException
import com.concordium.wallet.core.security.KeystoreHelper
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.util.Log
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException

class AuthenticationManager(
    private val authKeyName: String,
) {
    private val authPreferences = AuthPreferences(App.appContext)

    // region Biometrics

    fun initBiometricAuth(
        password: String,
        cipher: Cipher,
    ): Boolean {
        try {
            authPreferences.setEncryptedPassword(
                authKeyName,
                EncryptedData(
                    ciphertext = cipher.doFinal(password.toByteArray()),
                    iv = cipher.iv,
                    transformation = cipher.algorithm,
                )
            )
            authPreferences.setUseBiometrics(authKeyName, true)
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
            KeystoreHelper().generateSecretKey(authKeyName)
            true
        } catch (e: KeystoreEncryptionException) {
            false
        }
    }

    /**
     * Can throw KeystoreEncryptionException or return null in case of KeyPermanentlyInvalidatedException
     */
    fun getBiometricsCipherForEncryption(): Cipher? {
        return KeystoreHelper().initCipherForEncryption(authKeyName)
    }

    /**
     * Can throw KeystoreEncryptionException or return null in case of KeyPermanentlyInvalidatedException
     */
    fun getBiometricsCipherForDecryption(): Cipher? {
        try {
            val cipher = KeystoreHelper().initCipherForDecryption(
                keyName = authKeyName,
                initVector = authPreferences.getEncryptedPassword(authKeyName).decodeIv(),
            )

            if (cipher == null) {
                authPreferences.setUseBiometrics(authKeyName, false)
            }

            return cipher
        } catch (e: KeystoreEncryptionException) {
            authPreferences.setUseBiometrics(authKeyName, false)
            throw e
        }
    }

    suspend fun decryptPasswordWithBiometricsCipher(cipher: Cipher): String? {
        val encryptedPassword = authPreferences.getEncryptedPassword(authKeyName)
        val decryptedPassword = runCatching { cipher.doFinal(encryptedPassword.decodeCipherText()) }
            .getOrNull()
            ?.let(::String)
        return if (decryptedPassword != null && checkPassword(decryptedPassword)) {
            decryptedPassword
        } else {
            null
        }
    }

    //endregion

    @Throws(EncryptionException::class)
    suspend fun initPasswordAuth(password: String) =
        initPasswordAuth(
            password = password,
            masterKey = EncryptionHelper.generateMasterKey(),
        )

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
            authKeyName,
            passwordKeySalt
        )
        authPreferences.setEncryptedMasterKey(
            authKeyName,
            encryptedMasterKey
        )
    }

    @Throws(EncryptionException::class)
    suspend fun getMasterKey(
        password: String,
    ): ByteArray {
        val encryptedMasterKey = authPreferences.getEncryptedMasterKey(authKeyName)
        val passwordKey = EncryptionHelper.generatePasswordKey(
            password = password.toCharArray(),
            salt = authPreferences.getPasswordKeySalt(authKeyName),
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
        return authPreferences.getUsePasscode(authKeyName)
    }

    fun useBiometrics(): Boolean {
        return authPreferences.getUseBiometrics(authKeyName)
    }

    fun setUsePassCode(passcodeUsed: Boolean) {
        authPreferences.setUsePasscode(authKeyName, passcodeUsed)
    }
}
