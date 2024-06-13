package com.concordium.wallet.data.preferences

import android.content.Context
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import com.concordium.wallet.App
import com.concordium.wallet.util.toHex
import com.walletconnect.util.hexToBytes
import javax.crypto.SecretKey

class AuthPreferences(val context: Context) :
    Preferences(context, SharedPreferencesKeys.PREF_FILE_AUTH.key, Context.MODE_PRIVATE) {

    companion object {
        const val PREFKEY_HAS_SETUP_USER = "PREFKEY_HAS_SETUP_USER"
        const val PREFKEY_HAS_COMPLETED_INITIAL_SETUP = "PREFKEY_HAS_COMPLETED_INITIAL_SETUP"
        const val PREFKEY_USE_PASSCODE = "PREFKEY_USE_PASSCODE"
        const val PREFKEY_USE_BIOMETRICS = "PREFKEY_USE_BIOMETRICS"
        const val PREFKEY_PASSWORD_CHECK = "PREFKEY_PASSWORD_CHECK"
        const val PREFKEY_PASSWORD_CHECK_ENCRYPTED = "PREFKEY_PASSWORD_CHECK_ENCRYPTED"
        const val PREFKEY_PASSWORD_ENCRYPTION_SALT = "PREFKEY_PASSWORD_ENCRYPTION_SALT"
        const val PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR = "PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR"
        const val PREFKEY_ENCRYPTED_PASSWORD = "PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY"
        const val PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR =
            "PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR"
        const val PREFKEY_BIOMETRIC_KEY = "PREFKEY_BIOMETRIC_KEY"
        const val PREFKEY_ACCOUNTS_BACKED_UP = "PREFKEY_ACCOUNTS_BACKED_UP"
        const val PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX =
            "PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX"
        const val PREFKEY_LEGACY_SEED_HEX_ENCRYPTED = "SEED_PHRASE_ENCRYPTED"
        const val PREFKEY_TRACKING_ENABLED = "TRACKING_ENABLED"
    }

    fun setTrackingEnabled(value: Boolean) {
        setBoolean(PREFKEY_TRACKING_ENABLED, value)
    }

    fun isTrackingEnabled() =
        getBoolean(PREFKEY_TRACKING_ENABLED, false)

    fun setHasSetupUser(value: Boolean) {
        setBoolean(PREFKEY_HAS_SETUP_USER, value)
    }

    fun getHasSetupUser(): Boolean {
        return getBoolean(PREFKEY_HAS_SETUP_USER)
    }

    fun setHasCompletedInitialSetup(value: Boolean) {
        setBoolean(PREFKEY_HAS_COMPLETED_INITIAL_SETUP, value)
    }

    fun getHasCompletedInitialSetup(): Boolean {
        // Default value is true for backward compatibility.
        return getBoolean(PREFKEY_HAS_COMPLETED_INITIAL_SETUP, true)
    }

    fun setUsePasscode(appendix: String, value: Boolean) {
        setBoolean(PREFKEY_USE_PASSCODE + appendix, value)
    }

    fun getUsePasscode(appendix: String): Boolean {
        return getBoolean(PREFKEY_USE_PASSCODE + appendix)
    }

    fun setUseBiometrics(appendix: String, value: Boolean) {
        setBoolean(PREFKEY_USE_BIOMETRICS + appendix, value)
    }

    fun getUseBiometrics(appendix: String): Boolean {
        return getBoolean(PREFKEY_USE_BIOMETRICS + appendix)
    }

    fun setPasswordCheck(appendix: String, value: String) {
        setString(PREFKEY_PASSWORD_CHECK + appendix, value)
    }

    fun getPasswordCheck(appendix: String): String? {
        return getString(PREFKEY_PASSWORD_CHECK + appendix)
    }

    fun setPasswordCheckEncrypted(appendix: String, value: String) {
        setString(PREFKEY_PASSWORD_CHECK_ENCRYPTED + appendix, value)
    }

    fun getPasswordCheckEncrypted(appendix: String): String {
        return getString(PREFKEY_PASSWORD_CHECK_ENCRYPTED + appendix, "")
    }

    fun setPasswordEncryptionSalt(appendix: String, value: String) {
        setString(PREFKEY_PASSWORD_ENCRYPTION_SALT + appendix, value)
    }

    fun getPasswordEncryptionSalt(appendix: String): String {
        return getString(PREFKEY_PASSWORD_ENCRYPTION_SALT + appendix, "")
    }

    fun setPasswordEncryptionInitVector(appendix: String, value: String) {
        setString(PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR + appendix, value)
    }

    fun getPasswordEncryptionInitVector(appendix: String): String {
        return getString(PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR + appendix, "")
    }

    fun setEncryptedPassword(appendix: String, value: String) {
        setString(PREFKEY_ENCRYPTED_PASSWORD + appendix, value)
    }

    fun getEncryptedPassword(appendix: String): String {
        return getString(PREFKEY_ENCRYPTED_PASSWORD + appendix, "")
    }

    fun setEncryptedPasswordDerivedKeyInitVector(appendix: String, value: String) {
        setString(PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR + appendix, value)
    }

    fun getBiometricsKeyEncryptionInitVector(appendix: String): String {
        return getString(PREFKEY_ENCRYPTED_PASSWORD_DERIVED_KEY_INITVECTOR + appendix, "")
    }

    fun getAuthKeyName(): String {
        return getString(PREFKEY_BIOMETRIC_KEY, "default_key")
    }

    fun setAuthKeyName(key: String) {
        return setString(PREFKEY_BIOMETRIC_KEY, key)
    }

    fun isAccountsBackedUp(): Boolean {
        return getBoolean(PREFKEY_ACCOUNTS_BACKED_UP, true)
    }

    fun setAccountsBackedUp(value: Boolean) {
        return setBoolean(PREFKEY_ACCOUNTS_BACKED_UP, value)
    }

    fun addAccountsBackedUpListener(listener: Listener) {
        addListener(PREFKEY_ACCOUNTS_BACKED_UP, listener)
    }

    /**
     * Saves the seed phrase as its entropy, so it is possible to later get
     * both the seed hex and the seed phrase.
     *
     * @see getSeedHex
     * @see getSeedPhrase
     */
    suspend fun tryToSetEncryptedSeedPhrase(seedPhraseString: String, password: String): Boolean {
        val entropyHex = Mnemonics.MnemonicCode(seedPhraseString).toEntropy().toHex()
        val encryptedEntropyHex = App.appCore.getCurrentAuthenticationManager()
            .encryptInBackground(password, entropyHex)
            ?: return false
        return setStringWithResult(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX, encryptedEntropyHex)
    }

    suspend fun getSeedHex(password: String): String {
        val authenticationManager = App.appCore.getOriginalAuthenticationManager()

        // Try the encrypted entropy hex.
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX)
            ?.let { authenticationManager.decryptInBackground(password, it) }
            ?.let { Mnemonics.MnemonicCode(it.hexToBytes()).toSeed().toHex() }
            ?.let { return it }

        // Try the legacy encrypted seed hex.
        getString(PREFKEY_LEGACY_SEED_HEX_ENCRYPTED)
            ?.let { authenticationManager.decryptInBackground(password, it) }
            ?.let { return it }

        error("Failed to get the seed")
    }

    suspend fun getSeedHex(decryptKey: SecretKey): String? {
        val authenticationManager = App.appCore.getOriginalAuthenticationManager()

        // Try the encrypted entropy hex.
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX)
            ?.let { authenticationManager.decryptInBackground(decryptKey, it) }
            ?.let { Mnemonics.MnemonicCode(it.hexToBytes()).toSeed().toHex() }
            ?.let { return it }

        // Try the legacy encrypted seed hex.
        getString(PREFKEY_LEGACY_SEED_HEX_ENCRYPTED)
            ?.let { authenticationManager.decryptInBackground(decryptKey, it) }
            ?.let { return it }

        return null
    }

    /**
     * @see hasEncryptedSeedPhrase
     */
    suspend fun getSeedPhrase(password: String): String =
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX)
            ?.let {
                App.appCore.getOriginalAuthenticationManager().decryptInBackground(password, it)
            }
            ?.let {
                Mnemonics.MnemonicCode(it.hexToBytes()).words.joinToString(
                    separator = " ",
                    transform = CharArray::concatToString
                )
            }
            ?: error("Failed to get the seed phrase")

    /**
     * @see hasEncryptedSeedPhrase
     */
    suspend fun getSeedPhrase(decryptKey: SecretKey): String =
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX)
            ?.let {
                App.appCore.getOriginalAuthenticationManager().decryptInBackground(decryptKey, it)
            }
            ?.let {
                Mnemonics.MnemonicCode(it.hexToBytes()).words.joinToString(
                    separator = " ",
                    transform = CharArray::concatToString
                )
            }
            ?: error("Failed to get the seed phrase")

    fun updateEncryptedSeedHex(encryptedSeedHex: String): Boolean {
        return setStringWithResult(PREFKEY_LEGACY_SEED_HEX_ENCRYPTED, encryptedSeedHex)
    }

    fun updateEncryptedSeedEntropyHex(encryptedSeedEntropyHex: String): Boolean {
        return setStringWithResult(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX, encryptedSeedEntropyHex)
    }

    fun hasEncryptedSeed(): Boolean =
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX) != null
                || getString(PREFKEY_LEGACY_SEED_HEX_ENCRYPTED) != null

    /**
     * The seed phrase can be restored only if it has been saved as an entropy.
     *
     * @see getSeedPhrase
     */
    fun hasEncryptedSeedPhrase(): Boolean =
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX) != null
}
