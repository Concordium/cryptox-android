package com.concordium.wallet.data.preferences

import android.content.Context
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

    fun getIdentityCreationData(): IdentityCreationData? =
        getJsonSerialized<IdentityCreationData>(PREFKEY_IDENTITY_CREATION_DATA)

    fun setIdentityCreationData(data: IdentityCreationData?) =
        setJsonSerialized(PREFKEY_IDENTITY_CREATION_DATA, data)

    fun getShowForFirstIdentityFromCallback(): Boolean {
        return getBoolean(PREFKEY_SHOW_FOR_FIRST_IDENTITY, false)
    }

    fun setShowForFirstIdentityFromCallback(isFirst: Boolean) {
        setBoolean(PREFKEY_SHOW_FOR_FIRST_IDENTITY, isFirst)
    }

    private companion object {
        const val PREFKEY_IDENTITY_CREATION_DATA = "KEY_IDENTITY_CREATION_DATA"
        const val PREFKEY_SHOW_FOR_FIRST_IDENTITY = "SHOW_FOR_FIRST_IDENTITY"
    }
}
