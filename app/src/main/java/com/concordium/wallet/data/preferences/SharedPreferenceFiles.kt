package com.concordium.wallet.data.preferences

enum class SharedPreferenceFiles(val key: String) {
    APP_SETUP("PREF_FILE_APP_SETUP"),
    APP_TRACKING("PREF_TRACKING"),

    WALLET_SETUP("PREF_FILE_WALLET_SETUP"),
    WALLET_FILTER("PREF_FILE_FILTER"),
    WALLET_PROVIDER("PREF_FILE_PROVIDER"),
    WALLET_ID_CREATION_DATA("KEY_IDENTITY_CREATION_DATA"),
    WALLET_NOTIFICATIONS("PREF_NOTIFICATION"),
    WALLET_SEND_FUNDS("PREF_SEND_FUNDS"),
    ;
}
