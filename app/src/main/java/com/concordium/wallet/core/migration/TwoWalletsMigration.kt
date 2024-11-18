package com.concordium.wallet.core.migration

import android.content.Context
import android.util.Base64
import com.concordium.wallet.core.security.KeystoreHelper
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.preferences.AppSetupPreferences
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.preferences.WalletSetupPreferences

class TwoWalletsMigration(
    context: Context,
) {
    private val oldAuthPreferences: OldAuthPreferences by lazy {
        OldAuthPreferences(context)
    }

    private val oldEncryptionIv: ByteArray? by lazy {
        oldAuthPreferences.passwordEncryptionInitVectorBase64
            ?.let(::decodeOldBase64)
    }

    fun migrateOldEncryptedData(oldEncryptedDataBase64: String) =
        EncryptedData(
            ciphertext = decodeOldBase64(oldEncryptedDataBase64),
            iv = checkNotNull(oldEncryptionIv) {
                "Can't migrate old encrypted data due to the missing old encryption IV"
            },
            transformation = OLD_ENCRYPTION_TRANSFORMATION,
        )

    fun migrateOldPreferences(
        newWalletSetupPreferences: WalletSetupPreferences,
        newAppSetupPreferences: AppSetupPreferences,
    ) {
        // Auth.
        val authSlot = oldAuthPreferences.authKeyName
        newAppSetupPreferences.setCurrentAuthSlot(authSlot)
        newAppSetupPreferences.setUsePasscode(authSlot, oldAuthPreferences.usePasscode)
        newAppSetupPreferences.setUseBiometrics(authSlot, oldAuthPreferences.useBiometrics)
        oldAuthPreferences.passwordEncryptionSaltBase64
            ?.let(::decodeOldBase64)
            ?.also { newAppSetupPreferences.setPasswordKeySalt(authSlot, it) }
        oldAuthPreferences.encryptedPasswordBase64
            ?.let(::decodeOldBase64)
            ?.also { encryptedPassword ->
                val encryptedPasswordIv = oldAuthPreferences.encryptedPasswordIvBase64
                    ?.let(::decodeOldBase64)
                    ?: error("Can't save the encrypted password: missing IV")
                newAppSetupPreferences.setEncryptedPassword(
                    authSlot,
                    EncryptedData(
                        ciphertext = encryptedPassword,
                        iv = encryptedPasswordIv,
                        transformation = KeystoreHelper.ENCRYPTION_CIPHER_TRANSFORMATION,
                    )
                )
            }

        // App setup.
        newAppSetupPreferences.setHasCompletedInitialSetup(oldAuthPreferences.hasCompletedInitialSetup)

        // Wallet setup.
        newWalletSetupPreferences.setHasCompletedOnboarding(oldAuthPreferences.hasCompletedOnboarding)
        newWalletSetupPreferences.setAccountsBackedUp(oldAuthPreferences.areAccountsBackedUp)
        newWalletSetupPreferences.setHasShownInitialAnimation(oldAuthPreferences.hasShownInitialAnimation)
        oldAuthPreferences.encryptedSeedEntropyHexBase64
            ?.let(::migrateOldEncryptedData)
            ?.also { newEncryptedSeedEntropyHex ->
                check(
                    newWalletSetupPreferences.tryToSetEncryptedSeedPhrase(newEncryptedSeedEntropyHex)
                ) { "Failed setting encrypted seed phrase" }
            }
        oldAuthPreferences.encryptedSeedHexBase64
            ?.let(::migrateOldEncryptedData)
            ?.also { newEncryptedSeedHex ->
                check(
                    newWalletSetupPreferences.tryToSetEncryptedSeedHex(newEncryptedSeedHex)
                ) { "Failed setting encrypted seed" }
            }
    }

    private class OldAuthPreferences(
        context: Context,
    ) : Preferences(context, "PREF_FILE_AUTH") {

        val authKeyName: String
            get() = getString("PREFKEY_BIOMETRIC_KEY", "default_key")

        val passwordEncryptionInitVectorBase64: String?
            get() = getString("PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR$authKeyName")

        val passwordEncryptionSaltBase64: String?
            get() = getString("PREFKEY_PASSWORD_ENCRYPTION_SALT$authKeyName")

        val usePasscode: Boolean
            get() = getBoolean("PREFKEY_USE_PASSCODE$authKeyName", false)

        val useBiometrics: Boolean
            get() = getBoolean("PREFKEY_USE_PREFKEY_USE_BIOMETRICS$authKeyName", false)

        val passwordCheck: String?
            get() = getString("PREFKEY_PASSWORD_CHECK$authKeyName")

        val encryptedPasswordCheckBase64: String?
            get() = getString("PREFKEY_PASSWORD_CHECK_ENCRYPTED$authKeyName")

        val encryptedPasswordBase64: String?
            get() = getString("PREFKEY_ENCRYPTED_PASSWORD$authKeyName")

        val encryptedPasswordIvBase64: String?
            get() = getString("PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR$authKeyName")

        val hasCompletedInitialSetup: Boolean
            get() = getBoolean("PREFKEY_HAS_COMPLETED_INITIAL_SETUP", true)

        val hasCompletedOnboarding: Boolean
            get() = getBoolean("PREFKEY_HAS_COMPLETED_ONBOARDING", false)

        val areAccountsBackedUp: Boolean
            get() = getBoolean("PREFKEY_ACCOUNTS_BACKED_UP", false)

        val encryptedSeedEntropyHexBase64: String?
            get() = getString("PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX")

        val encryptedSeedHexBase64: String?
            get() = getString("SEED_PHRASE_ENCRYPTED")

        val hasShownInitialAnimation: Boolean
            get() = getBoolean("PREFKEY_HAS_SHOWED_INITIAL_ANIMATION", false)
    }

    companion object {
        const val OLD_ENCRYPTION_TRANSFORMATION = "AES/CBC/PKCS7Padding"

        fun decodeOldBase64(encoded: String): ByteArray =
            Base64.decode(encoded, Base64.DEFAULT)
    }
}
