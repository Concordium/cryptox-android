package com.concordium.wallet.data.preferences

import android.content.Context
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import com.concordium.wallet.App
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.util.toHex
import com.google.gson.Gson
import okio.ByteString.Companion.decodeHex

class WalletSetupPreferences
@Deprecated(
    message = "Do not construct instances on your own",
    replaceWith = ReplaceWith(
        expression = "App.appCore.session.walletStorage.setupPreferences",
        imports = arrayOf("com.concordium.wallet.App"),
    )
)
constructor(
    context: Context,
    fileNameSuffix: String = "",
    private val gson: Gson = App.appCore.gson,
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
            // The entropy is encoded to hex to keep backward compatibility
            // with the data encrypted before the Two wallets feature.
            data = entropy.toHex().toByteArray(),
        ) ?: return false
        return tryToSetEncryptedSeedPhrase(encryptedEntropy)
    }

    fun tryToSetEncryptedSeedPhrase(encryptedSeedEntropy: EncryptedData): Boolean =
        setStringWithResult(
            PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX_JSON,
            gson.toJson(encryptedSeedEntropy)
        )

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
            // The seed is not decoded from hex to keep backward compatibility
            // with the data encrypted before the Two wallets feature.
            data = seedHex.toByteArray(),
        ) ?: return false
        return tryToSetEncryptedSeedHex(encryptedSeed)
    }

    fun tryToSetEncryptedSeedHex(encryptedSeedHex: EncryptedData): Boolean =
        setStringWithResult(
            PREFKEY_ENCRYPTED_SEED_HEX_JSON,
            gson.toJson(encryptedSeedHex)
        )

    suspend fun getSeedHex(password: String): String {
        val authenticationManager = App.appCore.auth

        // Try the encrypted entropy.
        getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX_JSON, gson)
            ?.let { authenticationManager.decrypt(password, it) }
            ?.let { String(it).decodeHex().toByteArray() }
            ?.let { Mnemonics.MnemonicCode(it).toSeed().toHex() }
            ?.also { return it }

        // Try the encrypted seed.
        getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_SEED_HEX_JSON, gson)
            ?.let { authenticationManager.decrypt(password, it) }
            ?.let(::String)
            ?.also { return it }

        error("Failed to get the seed")
    }

    /**
     * @see hasEncryptedSeedPhrase
     */
    suspend fun getSeedPhrase(
        password: String,
        encryptedData: EncryptedData? = getEncryptedSeedPhrase()
    ): String =
        encryptedData
            ?.let { App.appCore.auth.decrypt(password, it) }
            ?.let { String(it).decodeHex().toByteArray() }
            ?.let {
                Mnemonics.MnemonicCode(it).words.joinToString(
                    separator = " ",
                    transform = CharArray::concatToString
                )
            }
            ?: error("Failed to get the seed phrase")

    fun getEncryptedSeedPhrase() =
        getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX_JSON, gson)

    fun hasEncryptedSeed(): Boolean =
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX_JSON) != null
                || getString(PREFKEY_ENCRYPTED_SEED_HEX_JSON) != null

    /**
     * The seed phrase can be restored only if it has been saved as an entropy.
     *
     * @see getSeedPhrase
     */
    fun hasEncryptedSeedPhrase(): Boolean =
        getString(PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX_JSON) != null

    fun setHasCompletedOnboarding(value: Boolean) {
        setBoolean(PREFKEY_HAS_COMPLETED_ONBOARDING, value)
    }

    fun getHasCompletedOnboarding(): Boolean {
        return getBoolean(PREFKEY_HAS_COMPLETED_ONBOARDING, false)
    }

    fun setHasShownInitialAnimation(value: Boolean) {
        setBoolean(PREFKEY_HAS_SHOWN_INITIAL_ANIMATION, value)
    }

    fun getHasShownInitialAnimation(): Boolean {
        return getBoolean(PREFKEY_HAS_SHOWN_INITIAL_ANIMATION, false)
    }

    fun setShowOnrampBanner(value: Boolean) {
        setBoolean(PREFKEY_SHOW_ONRAMP_BANNER, value)
    }

    fun getShowOnrampBanner(): Boolean {
        return getBoolean(PREFKEY_SHOW_ONRAMP_BANNER, true)
    }

    fun setShowEarnBanner(show: Boolean) {
        setBoolean(PREFKEY_SHOW_EARN_BANNER, show)
    }

    fun getShowEarnBanner(): Boolean {
        return getBoolean(PREFKEY_SHOW_EARN_BANNER, true)
    }

    fun setHasBackedUpWithDrive(value: Boolean) {
        setBoolean(PREFKEY_HAS_BACKED_UP_WITH_DRIVE, value)
    }

    fun getHasBackedUpWithDrive(): Boolean {
        return getBoolean(PREFKEY_HAS_BACKED_UP_WITH_DRIVE, false)
    }

    private companion object {
        const val PREFKEY_ACCOUNTS_BACKED_UP = "PREFKEY_ACCOUNTS_BACKED_UP"
        const val PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX_JSON =
            "PREFKEY_ENCRYPTED_SEED_ENTROPY_HEX_JSON"
        const val PREFKEY_ENCRYPTED_SEED_HEX_JSON = "PREFKEY_ENCRYPTED_SEED_HEX_JSON"
        const val PREFKEY_HAS_COMPLETED_ONBOARDING = "PREFKEY_HAS_COMPLETED_ONBOARDING"
        const val PREFKEY_HAS_SHOWN_INITIAL_ANIMATION = "PREFKEY_HAS_SHOWN_INITIAL_ANIMATION"
        const val PREFKEY_SHOW_ONRAMP_BANNER = "PREFKEY_SHOW_ONRAMP_BANNER"
        const val PREFKEY_SHOW_EARN_BANNER = "PREFKEY_SHOW_EARN_BANNER"
        const val PREFKEY_HAS_BACKED_UP_WITH_DRIVE = "PREFKEY_HAS_BACKED_UP_WITH_DRIVE"
    }
}
