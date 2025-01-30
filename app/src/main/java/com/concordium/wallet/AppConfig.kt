package com.concordium.wallet

object AppConfig {

    const val useOfflineMock = BuildConfig.USE_BACKEND_MOCK

    val appVersion: String
        get() = if (!BuildConfig.ENV_NAME.equals("production")) {
            BuildConfig.VERSION_NAME + (if (BuildConfig.DEBUG) " (debug)" else " (release)") +
                    " (" + BuildConfig.ENV_NAME + ")"
        } else BuildConfig.VERSION_NAME

    val net: String
        get() = if (BuildConfig.ENV_NAME == "production") "Mainnet" else "Testnet"
}
