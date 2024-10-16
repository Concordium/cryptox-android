package com.concordium.wallet.data.preferences

import android.content.Context
import com.concordium.wallet.App
import com.concordium.wallet.data.model.IdentityCreationData

class WalletIdentityCreationDataPreferences
@Deprecated(
    message = "Do not construct instances on your own",
    replaceWith = ReplaceWith(
        expression = "App.appCore.session.walletStorage.identityCreationDataPreferences",
        imports = arrayOf("com.concordium.wallet.App"),
    )
)
constructor(
    context: Context,
    fileNameSuffix: String = "",
) : Preferences(context, SharedPreferenceFiles.WALLET_ID_CREATION_DATA.key + fileNameSuffix) {

    private val gson = App.appCore.gson

    fun getIdentityCreationData(): IdentityCreationData? {
        return getJsonSerialized<StoredIdentityCreationData>(PREFKEY_IDENTITY_CREATION_DATA, gson)
            ?.let { storedData ->
                when (val version = storedData.version) {
                    0 -> gson.fromJson(storedData.data, IdentityCreationData.V0::class.java)
                    1 -> gson.fromJson(storedData.data, IdentityCreationData.V1::class.java)
                    else -> error("Unexpected version $version")
                }
            }
    }

    fun setIdentityCreationData(data: IdentityCreationData?) {
        setJsonSerialized(
            PREFKEY_IDENTITY_CREATION_DATA,
            when (data) {
                is IdentityCreationData.V0 ->
                    StoredIdentityCreationData(
                        version = 0,
                        data = gson.toJson(data)
                    )

                is IdentityCreationData.V1 ->
                    StoredIdentityCreationData(
                        version = 1,
                        data = gson.toJson(data)
                    )

                null ->
                    null
            },
            gson
        )
    }

    fun getShowForFirstIdentityFromCallback(): Boolean {
        return getBoolean(PREFKEY_SHOW_FOR_FIRST_IDENTITY, false)
    }

    fun setShowForFirstIdentityFromCallback(isFirst: Boolean) {
        setBoolean(PREFKEY_SHOW_FOR_FIRST_IDENTITY, isFirst)
    }

    private inner class StoredIdentityCreationData(
        val version: Int,
        val data: String,
    )

    private companion object {
        const val PREFKEY_IDENTITY_CREATION_DATA = "KEY_IDENTITY_CREATION_DATA"
        const val PREFKEY_SHOW_FOR_FIRST_IDENTITY = "SHOW_FOR_FIRST_IDENTITY"
    }
}
