package com.concordium.wallet.data.preferences

import android.content.Context

class TrackingPreferences(context: Context) :
    Preferences(context, SharedPreferencesKeys.PREF_TRACKING.key, Context.MODE_PRIVATE) {

    var isTrackingEnabled: Boolean
            by BooleanPreference(PREFKEY_TRACKING_ENABLED, false)

    var hasDecidedOnPermission: Boolean
            by BooleanPreference(PREFKEY_HAS_DECIDED_ON_PERMISSION, false)

    private companion object {
        private const val PREFKEY_TRACKING_ENABLED = "TRACKING_ENABLED"
        private const val PREFKEY_HAS_DECIDED_ON_PERMISSION = "HAS_DECIDED_ON_PERMISSION"
    }
}
