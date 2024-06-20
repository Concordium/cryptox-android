package com.concordium.wallet.data.preferences

import android.content.Context

class NotificationsPreferences(context: Context) :
    Preferences(context, SharedPreferencesKeys.PREF_NOTIFICATION.key, Context.MODE_PRIVATE) {

    var hasEverShownPermissionDialog: Boolean
            by BooleanPreference(PREFKEY_HAS_EVER_SHOWN_PERMISSION_DIALOG, false)

    var areCcdTxNotificationsEnabled: Boolean
            by BooleanPreference(PREFKEY_ARE_CCD_TX_NOTIFICATIONS_ENABLED, false)

    var areCis2TxNotificationsEnabled: Boolean
            by BooleanPreference(PREFKEY_ARE_CIS2_TX_NOTIFICATIONS_ENABLED, false)

    fun enableAll() {
        areCcdTxNotificationsEnabled = true
        areCis2TxNotificationsEnabled = true
    }

    private companion object {
        private const val PREFKEY_HAS_EVER_SHOWN_PERMISSION_DIALOG =
            "HAS_EVER_SHOWN_PERMISSION_DIALOG"
        private const val PREFKEY_ARE_CCD_TX_NOTIFICATIONS_ENABLED =
            "CCD_TX_NOTIFICATIONS_ENABLED"
        private const val PREFKEY_ARE_CIS2_TX_NOTIFICATIONS_ENABLED =
            "CIS2_TX_NOTIFICATIONS_ENABLED"
    }
}
