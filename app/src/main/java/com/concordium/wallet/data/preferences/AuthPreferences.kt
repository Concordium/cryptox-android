package com.concordium.wallet.data.preferences

import android.content.Context
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import com.concordium.wallet.App
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.util.toHex
import com.walletconnect.util.hexToBytes

class AuthPreferences(val context: Context) :
    Preferences(context, SharedPreferencesKeys.PREF_FILE_AUTH.key, Context.MODE_PRIVATE) {

    companion object {
        const val PREFKEY_HAS_SETUP_USER = "PREFKEY_HAS_SETUP_USER"
        const val PREFKEY_HAS_COMPLETED_INITIAL_SETUP = "PREFKEY_HAS_COMPLETED_INITIAL_SETUP"
        const val PREFKEY_USE_PASSCODE = "PREFKEY_USE_PASSCODE"
        const val PREFKEY_USE_BIOMETRICS = "PREFKEY_USE_BIOMETRICS"
        const val PREFKEY_PASSWORD_KEY_SALT_HEX = "PREFKEY_PASSWORD_KEY_SALT"
        const val PREFKEY_ENCRYPTED_PASSWORD_JSON = "PREFKEY_ENCRYPTED_PASSWORD_JSON"
        const val PREFKEY_AUTH_KEY = "PREFKEY_BIOMETRIC_KEY"
        const val PREFKEY_ACCOUNTS_BACKED_UP = "PREFKEY_ACCOUNTS_BACKED_UP"
        const val PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON = "PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON"
        const val PREFKEY_ENCRYPTED_SEED_JSON = "PREFKEY_ENCRYPTED_SEED_JSON"
        const val PREFKEY_ENCRYPTED_MASTER_KEY_JSON = "PREFKEY_ENCRYPTED_MASTER_KEY_JSON"
    }

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

    fun setPasswordKeySalt(appendix: String, value: ByteArray) {
        setString(PREFKEY_PASSWORD_KEY_SALT_HEX + appendix, value.toHex())
    }

    fun getPasswordKeySalt(appendix: String): ByteArray {
        return getString(PREFKEY_PASSWORD_KEY_SALT_HEX + appendix, "").hexToBytes()
    }

    fun setEncryptedPassword(appendix: String, value: EncryptedData) {
        setString(
            PREFKEY_ENCRYPTED_PASSWORD_JSON + appendix,
            App.appCore.gson.toJson(value)
        )
    }

    fun getEncryptedPassword(appendix: String): EncryptedData {
        return getString(PREFKEY_ENCRYPTED_PASSWORD_JSON + appendix)!!
            .let { App.appCore.gson.fromJson(it, EncryptedData::class.java) }
    }

    fun setEncryptedMasterKey(appendix: String, value: EncryptedData) {
        setString(
            PREFKEY_ENCRYPTED_MASTER_KEY_JSON + appendix,
            App.appCore.gson.toJson(value)
        )
    }

    fun getEncryptedMasterKey(appendix: String): EncryptedData {
        return getString(PREFKEY_ENCRYPTED_MASTER_KEY_JSON + appendix)!!
            .let { App.appCore.gson.fromJson(it, EncryptedData::class.java) }
    }

    fun getAuthKeyName(): String {
        return getString(PREFKEY_AUTH_KEY, "default_key")
    }

    fun setAuthKeyName(key: String) {
        return setString(PREFKEY_AUTH_KEY, key)
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
        val entropy = Mnemonics.MnemonicCode(seedPhraseString).toEntropy()
        val encryptedEntropy = App.appCore.getCurrentAuthenticationManager().encrypt(
            password = password,
            data = entropy,
        ) ?: return false
        return setStringWithResult(
            PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON,
            App.appCore.gson.toJson(encryptedEntropy)
        )
    }

    /**
     * Saves the seed in HEX. This method is **only** to be used in case of
     * recovering a wallet from a seed (Wallet private key).
     * In other situation, like creation of a new wallet or recovering it from a seed phrase,
     * [tryToSetEncryptedSeedPhrase] must be used instead.
     *
     * @see getSeedHex
     */
    suspend fun tryToSetEncryptedSeedHex(seedHex: String, password: String): Boolean {
        val encryptedSeed = App.appCore.getCurrentAuthenticationManager().encrypt(
            password = password,
            data = seedHex.hexToBytes(),
        ) ?: return false
        return setStringWithResult(
            PREFKEY_ENCRYPTED_SEED_JSON,
            App.appCore.gson.toJson(encryptedSeed)
        )
    }

    suspend fun getSeedHex(password: String): String {
        val authenticationManager = App.appCore.getOriginalAuthenticationManager()

        // Try the encrypted entropy.
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON)
            ?.let { App.appCore.gson.fromJson(it, EncryptedData::class.java) }
            ?.let { authenticationManager.decrypt(password, it) }
            ?.let { Mnemonics.MnemonicCode(it).toSeed().toHex() }
            ?.let { return it }

        // Try the encrypted seed.
        getString(PREFKEY_ENCRYPTED_SEED_JSON)
            ?.let { App.appCore.gson.fromJson(it, EncryptedData::class.java) }
            ?.let { authenticationManager.decrypt(password, it) }
            ?.let { return it.toHex() }

        error("Failed to get the seed")
    }

    /**
     * @see hasEncryptedSeedPhrase
     */
    suspend fun getSeedPhrase(password: String): String =
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON)
            ?.let { App.appCore.gson.fromJson(it, EncryptedData::class.java) }
            ?.let { App.appCore.getOriginalAuthenticationManager().decrypt(password, it) }
            ?.let {
                Mnemonics.MnemonicCode(it).words.joinToString(
                    separator = " ",
                    transform = CharArray::concatToString
                )
            }
            ?: error("Failed to get the seed phrase")

    fun hasEncryptedSeed(): Boolean =
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON) != null
                || getString(PREFKEY_ENCRYPTED_SEED_JSON) != null

    /**
     * The seed phrase can be restored only if it has been saved as an entropy.
     *
     * @see getSeedPhrase
     */
    fun hasEncryptedSeedPhrase(): Boolean =
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON) != null
}
