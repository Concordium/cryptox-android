package com.concordium.wallet.core.migration

import android.content.Context
import android.util.Base64
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.preferences.Preferences

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

    private class OldAuthPreferences(
        context: Context,
    ) : Preferences(context, "PREF_FILE_AUTH") {

        val authKeyName: String
            get() = getString("PREFKEY_BIOMETRIC_KEY", "default_key")

        val passwordEncryptionInitVectorBase64: String?
            get() = getString("PREFKEY_PASSWORD_ENCRYPTION_INITVECTOR$authKeyName")
    }

    companion object {
        const val OLD_ENCRYPTION_TRANSFORMATION = "AES/CBC/PKCS7Padding"

        fun decodeOldBase64(encoded: String): ByteArray =
            Base64.decode(encoded, Base64.DEFAULT)
    }
}
