package com.concordium.wallet.data.preferences

import android.content.Context
import com.concordium.wallet.ui.tokens.provider.ProviderMeta
import com.google.gson.Gson

class WalletProviderPreferences
@Deprecated(
    message = "Do not construct instances on your own",
    replaceWith = ReplaceWith(
        expression = "App.appCore.session.walletStorage.providerPreferences",
        imports = arrayOf("com.concordium.wallet.App"),
    )
)
constructor(
    val context: Context,
    fileNameSuffix: String = "",
) : Preferences(context, SharedPreferenceFiles.WALLET_PROVIDER.key + fileNameSuffix) {

    private val gson by lazy {
        Gson()
    }

    companion object {
        const val PREF_KEY_PROVIDERS = "PREF_KEY_PROVIDERS"
    }

    fun ifProviderNameExists(providerName: String): Boolean {
        val providers = getProviders()
        return providers.find { it.name == providerName } != null
    }

    fun removeProvider(providerMeta: ProviderMeta): Boolean {
        val providers = getProviders()
        val delRes = providers.removeIf {
            it.name == providerMeta.name && it.website == providerMeta.website
        }
        return if (delRes) {
            setProviders(providers)
            true
        } else {
            false
        }
    }

    fun setProviders(value: List<ProviderMeta>) {
        val json = gson.toJson(value)
        setString(PREF_KEY_PROVIDERS, json)
    }

    fun getProviders(): MutableList<ProviderMeta> {
        val json = getString(PREF_KEY_PROVIDERS)
        val res = gson.fromJson(json, Array<ProviderMeta>::class.java)?.toList() ?: emptyList()
        return res.toMutableList()
    }
}
