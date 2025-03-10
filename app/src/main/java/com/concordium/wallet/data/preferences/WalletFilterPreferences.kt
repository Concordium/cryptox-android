package com.concordium.wallet.data.preferences

import android.content.Context

class WalletFilterPreferences
@Deprecated(
    message = "Do not construct instances on your own",
    replaceWith = ReplaceWith(
        expression = "App.appCore.session.walletStorage.filterPreferences",
        imports = arrayOf("com.concordium.wallet.App"),
    )
)
constructor(
    val context: Context,
    fileNameSuffix: String = "",
) : Preferences(context, SharedPreferenceFiles.WALLET_FILTER.key + fileNameSuffix) {

    private companion object {
        const val PREFKEY_FILTER_SHOW_REWARDS = "PREFKEY_FILTER_SHOW_REWARDS"
        const val PREFKEY_FILTER_SHOW_FINALIZATION_REWARDS = "PREFKEY_FILTER_SHOW_FINALIZATION_REWARDS"
    }

    fun setHasShowRewards(id: Int, value: Boolean) {
        setBoolean(PREFKEY_FILTER_SHOW_REWARDS + id, value)
    }

    fun getHasShowRewards(id: Int): Boolean {
        return getBoolean(PREFKEY_FILTER_SHOW_REWARDS + id, true)
    }

    fun setHasShowFinalizationRewards(id: Int, value: Boolean) {
        setBoolean(PREFKEY_FILTER_SHOW_FINALIZATION_REWARDS + id, value)
    }

    fun getHasShowFinalizationRewards(id: Int): Boolean {
        return getBoolean(PREFKEY_FILTER_SHOW_FINALIZATION_REWARDS + id, true)
    }
}
