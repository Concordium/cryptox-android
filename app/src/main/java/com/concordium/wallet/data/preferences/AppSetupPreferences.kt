package com.concordium.wallet.data.preferences

import android.content.Context
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.util.toHex
import com.google.gson.Gson
import okio.ByteString.Companion.decodeHex

class AppSetupPreferences(
    context: Context,
    private val gson: Gson,
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
        return getString(PREFKEY_PASSWORD_KEY_SALT_HEX + slot, "").decodeHex().toByteArray()
    }

    fun hasPasswordKeySalt(slot: String): Boolean {
        return getString(PREFKEY_PASSWORD_KEY_SALT_HEX + slot) != null
    }

    fun setEncryptedPassword(slot: String, value: EncryptedData) {
        setJsonSerialized(PREFKEY_ENCRYPTED_PASSWORD_JSON + slot, value, gson)
    }

    fun getEncryptedPassword(slot: String): EncryptedData {
        return getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_PASSWORD_JSON + slot, gson)!!
    }

    fun setEncryptedMasterKey(slot: String, value: EncryptedData) {
        setJsonSerialized(PREFKEY_ENCRYPTED_MASTER_KEY_JSON + slot, value, gson)
    }

    fun getEncryptedMasterKey(slot: String): EncryptedData {
        return getJsonSerialized<EncryptedData>(PREFKEY_ENCRYPTED_MASTER_KEY_JSON + slot, gson)!!
    }

    fun hasEncryptedMasterKey(slot: String): Boolean {
        return getString(PREFKEY_ENCRYPTED_MASTER_KEY_JSON + slot) != null
    }

    fun setLegacyEncryptedPasswordCheck(slot: String, value: EncryptedData) {
        setJsonSerialized(PREFKEY_LEGACY_ENCRYPTED_PASSWORD_CHECK_JSON + slot, value, gson)
    }

    fun getLegacyEncryptedPasswordCheck(slot: String): EncryptedData? {
        return getJsonSerialized<EncryptedData>(
            PREFKEY_LEGACY_ENCRYPTED_PASSWORD_CHECK_JSON + slot,
            gson
        )
    }

    fun setLegacyPasswordCheck(slot: String, value: String) {
        setString(PREFKEY_LEGACY_PASSWORD_CHECK + slot, value)
    }

    fun getLegacyPasswordCheck(slot: String): String? {
        return getString(PREFKEY_LEGACY_PASSWORD_CHECK + slot)
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

    fun setHasShowReviewDialogAfterReceiveFunds(value: Boolean) {
        setBoolean(PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_RECEIVE_FUNDS, value)
    }

    fun getHasShowReviewDialogAfterReceiveFunds(): Boolean {
        return getBoolean(PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_RECEIVE_FUNDS, false)
    }

    fun setHasShowReviewDialogAfterSendFunds(value: Boolean) {
        setBoolean(PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_SEND_FUNDS, value)
    }

    fun getHasShowReviewDialogAfterSendFunds(): Boolean {
        return getBoolean(PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_SEND_FUNDS, false)
    }

    fun setHasShowReviewDialogAfterEarnSetup(value: Boolean) {
        setBoolean(PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_EARN_SETUP, value)
    }

    fun getHasShowReviewDialogAfterEarnSetup(): Boolean {
        return getBoolean(PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_EARN_SETUP, false)
    }

    private companion object {
        const val PREFKEY_USE_PASSCODE = "PREFKEY_USE_PASSCODE"
        const val PREFKEY_USE_BIOMETRICS = "PREFKEY_USE_BIOMETRICS"
        const val PREFKEY_PASSWORD_KEY_SALT_HEX = "PREFKEY_PASSWORD_KEY_SALT"
        const val PREFKEY_ENCRYPTED_PASSWORD_JSON = "PREFKEY_ENCRYPTED_PASSWORD_JSON"
        const val PREFKEY_CURRENT_AUTH_SLOT = "PREFKEY_CURRENT_AUTH_SLOT"
        const val PREFKEY_ENCRYPTED_MASTER_KEY_JSON = "PREFKEY_ENCRYPTED_MASTER_KEY_JSON"
        const val PREFKEY_HAS_COMPLETED_INITIAL_SETUP = "PREFKEY_HAS_COMPLETED_INITIAL_SETUP"
        const val PREFKEY_LEGACY_PASSWORD_CHECK = "PREFKEY_LEGACY_PASSWORD_CHECK"
        const val PREFKEY_LEGACY_ENCRYPTED_PASSWORD_CHECK_JSON =
            "PREFKEY_LEGACY_ENCRYPTED_PASSWORD_CHECK_JSON"
        const val PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_RECEIVE_FUNDS =
            "PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_RECEIVE_FUNDS"
        const val PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_SEND_FUNDS =
            "PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_SEND_FUNDS"
        const val PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_EARN_SETUP =
            "PREFKEY_HAS_SHOW_REVIEW_DIALOG_AFTER_EARN_SETUP"
    }
}
