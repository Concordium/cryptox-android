package com.concordium.wallet.ui.transaction.sendfunds

import android.content.Context
import com.concordium.wallet.data.preferences.Preferences

class SendFundsPreferences(context: Context, preferenceName: String = PREFERENCE_NAME) :
    Preferences(context, preferenceName, Context.MODE_PRIVATE) {
    fun showMemoWarning(): Boolean {
        return getBoolean(KEY_SHOW_MEMO_WARNING, true)
    }

    fun dontShowMemoWarning() {
        setBoolean(KEY_SHOW_MEMO_WARNING, false)
    }

    companion object {
        const val PREFERENCE_NAME = "PREF_SEND_FUNDS"
        private const val KEY_SHOW_MEMO_WARNING = "KEY_SHOW_MEMO_WARNING_V2"
    }
}