package com.concordium.wallet.data

import android.content.Context
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.multinetwork.AppNetwork
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
 * @param wallet the current active wallet. Storage is separated per wallet
 * @param network the current network. Some parts of a particular wallet storage are separated per network
 */
@Suppress("DEPRECATION")
class WalletStorage(
    wallet: AppWallet,
    network: AppNetwork,
    private val context: Context,
) {
    // Primary wallet has no suffix for backward compatibility.
    private val walletWideFileNameSuffix =
        if (wallet.isPrimary)
            ""
        else
            "_${wallet.id}"

    // Primary network for the build flavour has no suffix for backward compatibility.
    @Suppress("KotlinConstantConditions")
    private val networkSpecificFileNameSuffix =
        walletWideFileNameSuffix +
                if (BuildConfig.FLAVOR == "mainnet" && network.isMainnet
                    || BuildConfig.FLAVOR == "tstnet" && network.isTestnet
                )
                    ""
                else
                    "_${network.genesisHash}"

    val database: WalletDatabase by lazy {
        WalletDatabase.getDatabase(context, networkSpecificFileNameSuffix)
    }

    val filterPreferences: WalletFilterPreferences by lazy {
        WalletFilterPreferences(context, walletWideFileNameSuffix)
    }

    val sendFundsPreferences: WalletSendFundsPreferences by lazy {
        WalletSendFundsPreferences(context, walletWideFileNameSuffix)
    }

    val identityCreationDataPreferences: WalletIdentityCreationDataPreferences by lazy {
        WalletIdentityCreationDataPreferences(context, networkSpecificFileNameSuffix)
    }

    val notificationsPreferences: WalletNotificationsPreferences by lazy {
        WalletNotificationsPreferences(context, walletWideFileNameSuffix)
    }

    val providerPreferences: WalletProviderPreferences by lazy {
        WalletProviderPreferences(context, networkSpecificFileNameSuffix)
    }

    val setupPreferences: WalletSetupPreferences by lazy {
        WalletSetupPreferences(context, walletWideFileNameSuffix)
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
