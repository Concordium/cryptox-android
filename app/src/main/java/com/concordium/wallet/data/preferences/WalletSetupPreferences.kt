package com.concordium.wallet.data.preferences

import android.content.Context
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import com.concordium.wallet.App
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.util.toHex
import com.walletconnect.util.hexToBytes

class WalletSetupPreferences
@Deprecated(
    message = "Do not construct instances on your own",
    replaceWith = ReplaceWith(
        expression = "App.appCore.session.walletStorage.setupPreferences",
        imports = arrayOf("com.concordium.wallet.App"),
    )
)
constructor(
    val context: Context,
    fileNameSuffix: String = "",
) : Preferences(context, SharedPreferenceFiles.WALLET_SETUP.key + fileNameSuffix) {

    fun areAccountsBackedUp(): Boolean {
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
        val encryptedEntropy = App.appCore.auth.encrypt(
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
        val encryptedSeed = App.appCore.auth.encrypt(
            password = password,
            data = seedHex.hexToBytes(),
        ) ?: return false
        return setStringWithResult(
            PREFKEY_ENCRYPTED_SEED_JSON,
            App.appCore.gson.toJson(encryptedSeed)
        )
    }

    suspend fun getSeedHex(password: String): String {
        val authenticationManager = App.appCore.auth

        // Try the encrypted entropy.
        getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON)
            ?.let { authenticationManager.decrypt(password, it) }
            ?.let { Mnemonics.MnemonicCode(it).toSeed().toHex() }
            ?.let { return it }

        // Try the encrypted seed.
        getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_SEED_JSON)
            ?.let { authenticationManager.decrypt(password, it) }
            ?.let { return it.toHex() }

        error("Failed to get the seed")
    }

    /**
     * @see hasEncryptedSeedPhrase
     */
    suspend fun getSeedPhrase(password: String): String =
        getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON)
            ?.let { App.appCore.auth.decrypt(password, it) }
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

    private companion object {
        const val PREFKEY_ACCOUNTS_BACKED_UP = "PREFKEY_ACCOUNTS_BACKED_UP"
        const val PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON = "PREFKEY_ENCRYPTED_SEED_ENTROPY_JSON"
        const val PREFKEY_ENCRYPTED_SEED_JSON = "PREFKEY_ENCRYPTED_SEED_JSON"
    }
}