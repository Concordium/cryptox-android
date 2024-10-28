package com.concordium.wallet.data

import android.content.Context
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.preferences.WalletFilterPreferences
import com.concordium.wallet.data.preferences.WalletIdentityCreationDataPreferences
import com.concordium.wallet.data.preferences.WalletNotificationsPreferences
import com.concordium.wallet.data.preferences.WalletProviderPreferences
import com.concordium.wallet.data.preferences.WalletSendFundsPreferences
import com.concordium.wallet.data.preferences.WalletSetupPreferences
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A provider for all the persistence for a single wallet.
 *
 * @param fileNameSuffix a part to append to default persistence file names.
 * For the primary wallet, use an empty string to keep backward compatibility.
 */
@Suppress("DEPRECATION")
class WalletStorage(
    private val context: Context,
    private val fileNameSuffix: String,
) {
    constructor(
        context: Context,
        activeWallet: AppWallet,
    ) : this(
        context = context,
        fileNameSuffix =
        if (activeWallet.id.isEmpty())
            ""
        else
            "_${activeWallet.id}"
    )

    val database: WalletDatabase by lazy {
        WalletDatabase.getDatabase(context, fileNameSuffix)
    }

    val filterPreferences: WalletFilterPreferences by lazy {
        WalletFilterPreferences(context, fileNameSuffix)
    }

    val sendFundsPreferences: WalletSendFundsPreferences by lazy {
        WalletSendFundsPreferences(context, fileNameSuffix)
    }

    val identityCreationDataPreferences: WalletIdentityCreationDataPreferences by lazy {
        WalletIdentityCreationDataPreferences(context, fileNameSuffix)
    }

    val notificationsPreferences: WalletNotificationsPreferences by lazy {
        WalletNotificationsPreferences(context, fileNameSuffix)
    }

    val providerPreferences: WalletProviderPreferences by lazy {
        WalletProviderPreferences(context, fileNameSuffix)
    }

    val setupPreferences: WalletSetupPreferences by lazy {
        WalletSetupPreferences(context, fileNameSuffix)
    }

    suspend fun erase() = withContext(Dispatchers.IO) {
        database.clearAllTables()

        arrayOf(
            filterPreferences,
            sendFundsPreferences,
            identityCreationDataPreferences,
            notificationsPreferences,
            providerPreferences,
            setupPreferences,
        ).forEach(Preferences::clearAll)
    }
}
