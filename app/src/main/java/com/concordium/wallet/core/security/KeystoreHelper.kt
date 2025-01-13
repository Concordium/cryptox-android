package com.concordium.wallet.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.concordium.wallet.util.Log
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class KeystoreHelper {

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        const val ENCRYPTION_CIPHER_TRANSFORMATION =
            "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
    }

    private fun generateSecretKeyWithSpecs(keyGenParameterSpec: KeyGenParameterSpec) {
        val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEY_STORE)
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun generateSecretKey(keyName: String) {
        try {
            val keyProperties = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val builder = KeyGenParameterSpec.Builder(keyName, keyProperties)
                .setBlockModes(ENCRYPTION_BLOCK_MODE)
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setInvalidatedByBiometricEnrollment(true)

            generateSecretKeyWithSpecs(builder.build())
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchProviderException,
                is InvalidAlgorithmParameterException,
                is CertificateException,
                is IOException,
                -> {
                    Log.d("Failed to generate secret key", e)
                    throw KeystoreEncryptionException("Failed to generate secret key", e)
                }

                else -> throw e
            }
        }
    }

    private fun getCipher(): Cipher =
        Cipher.getInstance(ENCRYPTION_CIPHER_TRANSFORMATION)

    private fun getSecretKey(keyName: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        return keyStore.getKey(keyName, null) as SecretKey
    }

    fun initCipherForEncryption(keyName: String): Cipher? {
        try {
            val cipher = getCipher()
            val secretKey = getSecretKey(keyName)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return cipher
        } catch (e: Exception) {
            when (e) {
                is KeyPermanentlyInvalidatedException -> return null
                is KeyStoreException,
                is CertificateException,
                is UnrecoverableKeyException,
                is IOException,
                is NoSuchAlgorithmException,
                is NoSuchPaddingException,
                is InvalidKeyException,
                -> {
                    Log.d("Failed to init Cipher", e)
                    throw KeystoreEncryptionException("Failed to init Cipher", e)
                }

                else -> throw e
            }
        }
    }

    fun initCipherForDecryption(keyName: String, initVector: ByteArray): Cipher? {
        try {
            val cipher = getCipher()
            val secretKey = getSecretKey(keyName)
            val ivParams = IvParameterSpec(initVector)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams)
            return cipher
        } catch (e: Exception) {
            when (e) {
                is KeyPermanentlyInvalidatedException -> return null
                is KeyStoreException,
                is CertificateException,
                is UnrecoverableKeyException,
                is IOException,
                is NoSuchAlgorithmException,
                is NoSuchPaddingException,
                is InvalidAlgorithmParameterException,
                is InvalidKeyException,
                -> {
                    Log.d("Failed to init Cipher", e)
                    throw KeystoreEncryptionException("Failed to init Cipher", e)
                }

                else -> throw e
            }
        }
    }
}
