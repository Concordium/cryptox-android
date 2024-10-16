package com.concordium.wallet.data.preferences

import android.content.Context
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.util.toHex
import com.walletconnect.util.hexToBytes

class AppSetupPreferences(
    val context: Context,
) : Preferences(context, SharedPreferenceFiles.APP_SETUP.key) {

    fun setUsePasscode(slot: String, value: Boolean) {
        setBoolean(PREFKEY_USE_PASSCODE + slot, value)
    }

    fun getUsePasscode(slot: String): Boolean {
        return getBoolean(PREFKEY_USE_PASSCODE + slot)
    }

    fun setUseBiometrics(slot: String, value: Boolean) {
        setBoolean(PREFKEY_USE_BIOMETRICS + slot, value)
    }

    fun getUseBiometrics(slot: String): Boolean {
        return getBoolean(PREFKEY_USE_BIOMETRICS + slot)
    }

    fun setPasswordKeySalt(slot: String, value: ByteArray) {
        setString(PREFKEY_PASSWORD_KEY_SALT_HEX + slot, value.toHex())
    }

    fun getPasswordKeySalt(slot: String): ByteArray {
        return getString(PREFKEY_PASSWORD_KEY_SALT_HEX + slot, "").hexToBytes()
    }

    fun hasPasswordKeySalt(slot: String): Boolean {
        return getString(PREFKEY_PASSWORD_KEY_SALT_HEX + slot) != null
    }

    fun setEncryptedPassword(slot: String, value: EncryptedData) {
        setJsonSerialized(PREFKEY_ENCRYPTED_PASSWORD_JSON + slot, value)
    }

    fun getEncryptedPassword(slot: String): EncryptedData {
        return getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_PASSWORD_JSON + slot)!!
    }

    fun setEncryptedMasterKey(slot: String, value: EncryptedData) {
        setJsonSerialized(PREFKEY_ENCRYPTED_MASTER_KEY_JSON + slot, value)
    }

    fun getEncryptedMasterKey(slot: String): EncryptedData {
        return getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_MASTER_KEY_JSON + slot)!!
    }

    fun hasEncryptedMasterKey(slot: String): Boolean {
        return getString(PREFKEY_ENCRYPTED_MASTER_KEY_JSON + slot) != null
    }

    fun getCurrentAuthSlot(): String {
        return getString(PREFKEY_CURRENT_AUTH_SLOT, "default_key")
    }

    fun setCurrentAuthSlot(slot: String) {
        return setString(PREFKEY_CURRENT_AUTH_SLOT, slot)
    }

    fun setHasCompletedInitialSetup(value: Boolean) {
        setBoolean(PREFKEY_HAS_COMPLETED_INITIAL_SETUP, value)
    }

    fun getHasCompletedInitialSetup(): Boolean {
        return getBoolean(PREFKEY_HAS_COMPLETED_INITIAL_SETUP, false)
    }

    private companion object {
        const val PREFKEY_USE_PASSCODE = "PREFKEY_USE_PASSCODE"
        const val PREFKEY_USE_BIOMETRICS = "PREFKEY_USE_BIOMETRICS"
        const val PREFKEY_PASSWORD_KEY_SALT_HEX = "PREFKEY_PASSWORD_KEY_SALT"
        const val PREFKEY_ENCRYPTED_PASSWORD_JSON = "PREFKEY_ENCRYPTED_PASSWORD_JSON"
        const val PREFKEY_CURRENT_AUTH_SLOT = "PREFKEY_CURRENT_AUTH_SLOT"
        const val PREFKEY_ENCRYPTED_MASTER_KEY_JSON = "PREFKEY_ENCRYPTED_MASTER_KEY_JSON"
        const val PREFKEY_HAS_COMPLETED_INITIAL_SETUP = "PREFKEY_HAS_COMPLETED_INITIAL_SETUP"
    }
}
