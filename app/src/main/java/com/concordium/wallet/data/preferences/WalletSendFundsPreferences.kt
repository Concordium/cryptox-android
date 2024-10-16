package com.concordium.wallet.data.preferences

import android.content.Context

class WalletSendFundsPreferences
@Deprecated(
    message = "Do not construct instances on your own",
    replaceWith = ReplaceWith(
        expression = "App.appCore.session.walletStorage.sendFundsPreferences",
        imports = arrayOf("com.concordium.wallet.App"),
    )
)
constructor(
    context: Context,
    fileNameSuffix: String = "",
) : Preferences(context, SharedPreferenceFiles.WALLET_SEND_FUNDS.key + fileNameSuffix) {

    fun shouldShowMemoWarning(): Boolean {
        return getBoolean(KEY_SHOW_MEMO_WARNING, true)
    }

    fun disableShowMemoWarning() {
        setBoolean(KEY_SHOW_MEMO_WARNING, false)
    }

    private companion object {
        const val KEY_SHOW_MEMO_WARNING = "KEY_SHOW_MEMO_WARNING_V2"
    }
}
