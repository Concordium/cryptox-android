package com.concordium.wallet.data.preferences

import android.content.Context

class WalletNotificationsPreferences
@Deprecated(
    message = "Do not construct instances on your own",
    replaceWith = ReplaceWith(
        expression = "App.appCore.session.walletStorage.notificationsPreferences",
        imports = arrayOf("com.concordium.wallet.App"),
    )
)
constructor(
    context: Context,
    fileNameSuffix: String = "",
) : Preferences(context, SharedPreferenceFiles.WALLET_NOTIFICATIONS.key + fileNameSuffix) {

    var hasEverShownPermissionDialog: Boolean
            by BooleanPreference(PREFKEY_HAS_EVER_SHOWN_PERMISSION_DIALOG, false)

    var areCcdTxNotificationsEnabled: Boolean
            by BooleanPreference(PREFKEY_ARE_CCD_TX_NOTIFICATIONS_ENABLED, false)

    var areCis2TxNotificationsEnabled: Boolean
            by BooleanPreference(PREFKEY_ARE_CIS2_TX_NOTIFICATIONS_ENABLED, false)

    var arePltTxNotificationsEnabled: Boolean
            by BooleanPreference(PREFKEY_ARE_PLT_TX_NOTIFICATIONS_ENABLED, false)

    fun enableAll(areNotificationsEnabled: Boolean) {
        areCcdTxNotificationsEnabled = areNotificationsEnabled
        areCis2TxNotificationsEnabled = areNotificationsEnabled
        arePltTxNotificationsEnabled = areNotificationsEnabled
    }

    private companion object {
        private const val PREFKEY_HAS_EVER_SHOWN_PERMISSION_DIALOG =
            "HAS_EVER_SHOWN_PERMISSION_DIALOG"
        private const val PREFKEY_ARE_CCD_TX_NOTIFICATIONS_ENABLED =
            "CCD_TX_NOTIFICATIONS_ENABLED"
        private const val PREFKEY_ARE_CIS2_TX_NOTIFICATIONS_ENABLED =
            "CIS2_TX_NOTIFICATIONS_ENABLED"
        private const val PREFKEY_ARE_PLT_TX_NOTIFICATIONS_ENABLED =
            "PLT_TX_NOTIFICATIONS_ENABLED"
    }
}
