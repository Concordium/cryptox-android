package com.concordium.wallet.data

import android.content.Context
import com.concordium.wallet.data.preferences.WalletFilterPreferences
import com.concordium.wallet.data.preferences.WalletNotificationsPreferences
import com.concordium.wallet.data.preferences.WalletProviderPreferences
import com.concordium.wallet.data.preferences.WalletSetupPreferences
import com.concordium.wallet.data.room.WalletDatabase

class WalletStorage(
    private val context: Context,
) {
    val database: WalletDatabase by lazy {
        WalletDatabase.getDatabase(context)
    }

    val filterPreferences: WalletFilterPreferences by lazy {
        WalletFilterPreferences(context)
    }

    val notificationsPreferences: WalletNotificationsPreferences by lazy {
        WalletNotificationsPreferences(context)
    }

    val providerPreferences: WalletProviderPreferences by lazy {
        WalletProviderPreferences(context)
    }

    val setupPreferences: WalletSetupPreferences by lazy {
        WalletSetupPreferences(context)
    }
}
