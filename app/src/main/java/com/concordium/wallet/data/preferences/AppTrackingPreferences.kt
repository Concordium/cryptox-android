package com.concordium.wallet.data.preferences

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics

class AppTrackingPreferences(
    context: Context,
) : Preferences(context, SharedPreferenceFiles.APP_TRACKING.key) {

    var isTrackingEnabled: Boolean
            by BooleanPreference(PREFKEY_TRACKING_ENABLED, false)

    var hasDecidedOnPermission: Boolean
            by BooleanPreference(PREFKEY_HAS_DECIDED_ON_PERMISSION, false)

    init {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        addListener(PREFKEY_TRACKING_ENABLED) {
            firebaseAnalytics.setAnalyticsCollectionEnabled(isTrackingEnabled)
        }
    }

    private companion object {
        private const val PREFKEY_TRACKING_ENABLED = "TRACKING_ENABLED"
        private const val PREFKEY_HAS_DECIDED_ON_PERMISSION = "HAS_DECIDED_ON_PERMISSION"
    }
}
