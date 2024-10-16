package com.concordium.wallet.ui.transaction.sendfunds

import android.content.Context
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.preferences.SharedPreferenceFiles

class SendFundsPreferences(
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
