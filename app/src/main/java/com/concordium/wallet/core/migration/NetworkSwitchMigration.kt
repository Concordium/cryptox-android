package com.concordium.wallet.core.migration

import android.content.Context
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.data.room.app.AppDatabase
import com.concordium.wallet.data.room.app.AppNetworkDao
import com.concordium.wallet.data.room.app.AppNetworkEntity
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Date

class NetworkSwitchMigration(
    private val context: Context,
) {
    private val appNetworkDao: AppNetworkDao by lazy {
        AppDatabase.getDatabase(context).appNetworkDao()
    }

    suspend fun isAppDatabaseMigrationNeeded(): Boolean =
    // The migration is needed when updated to the "network switch" release
        // as well as when the app is just installed so the DB must be prepared.
        appNetworkDao.getCount() == 0

    suspend fun migrateAppDatabaseOnce() {
        check(isAppDatabaseMigrationNeeded()) {
            "The migration is not needed"
        }

        val predefinedNetworks = listOf(
            AppNetwork(
                genesisHash = "9dd9ca4d19e9393877d2c44b70f89acbfc0883c2243e5eeaecc0d1cd0503f478",
                name = "Mainnet",
                createdAt = Date(0),
                walletProxyUrl = "https://wallet-proxy.mainnet.concordium.software/".toHttpUrl(),
                ccdScanFrontendUrl = "https://ccdscan.io/".toHttpUrl(),
                ccdScanBackendUrl = "https://api-ccdscan.mainnet.concordium.software/rest/".toHttpUrl(),
                notificationsServiceUrl = "https://notification-api.mainnet.concordium.software/api/".toHttpUrl(),
            ),
            AppNetwork(
                genesisHash = "4221332d34e1694168c2a0c0b3fd0f273809612cb13d000d5c2e00e85f50f796",
                name = "Testnet",
                createdAt = Date(1),
                walletProxyUrl = "https://wallet-proxy.testnet.concordium.com/".toHttpUrl(),
                ccdScanFrontendUrl = "https://testnet.ccdscan.io/".toHttpUrl(),
                ccdScanBackendUrl = "https://api-ccdscan.testnet.concordium.com/rest/".toHttpUrl(),
                notificationsServiceUrl = "https://notification-api.testnet.concordium.com/api/".toHttpUrl(),
            ),
            AppNetwork(
                genesisHash = "38bf770b4c247f09e1b62982bb71000c516480c5a2c5214dadac6da4b1ad50e5",
                name = "Stagenet",
                createdAt = Date(2),
                walletProxyUrl = "https://wallet-proxy.stagenet.concordium.com/".toHttpUrl(),
                ccdScanFrontendUrl = "https://stagenet.ccdscan.io/".toHttpUrl(),
                ccdScanBackendUrl = null,
                notificationsServiceUrl = "https://notification-api.stagenet.concordium.com/api/".toHttpUrl(),
            ),
        )

        appNetworkDao.insert(
            networks =
                predefinedNetworks
                    .map { network ->
                        AppNetworkEntity(
                            network = network,
                            isActive = network.genesisHash == BuildConfig.DEFAULT_NETWORK_GENESIS_HASH,
                        )
                    }
                    .also { networksToInsert ->
                        check(networksToInsert.any(AppNetworkEntity::isActive)) {
                            "No predefined network to activate"
                        }
                    }
                    .toTypedArray()
        )
    }
}
