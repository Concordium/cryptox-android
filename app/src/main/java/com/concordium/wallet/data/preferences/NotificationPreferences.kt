package com.concordium.wallet.data.preferences

import android.content.Context

class NotificationPreferences(context: Context) :
    Preferences(context, SharedPreferencesKeys.PREF_NOTIFICATION.key, Context.MODE_PRIVATE) {

    var hasEverShownPermissionDialog: Boolean
        by BooleanPreference(PREFKEY_HAS_EVER_SHOWN_PERMISSION_DIALOG, false)

    private companion object {
        private const val PREFKEY_HAS_EVER_SHOWN_PERMISSION_DIALOG =
            "HAS_EVER_SHOWN_PERMISSION_DIALOG"
    }
}
