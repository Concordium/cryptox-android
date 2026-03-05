package com.concordium.wallet.data

import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.data.room.app.AppNetworkDao
import com.concordium.wallet.data.room.app.AppNetworkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl
import java.util.Date

class AppNetworkRepository(
    private val appNetworkDao: AppNetworkDao,
) {
    // Predefined networks are added by the Network switch migration.

    fun getNetworksFlow(): Flow<List<AppNetwork>> =
        appNetworkDao
            .getAll()
            .map { rows ->
                rows.map(AppNetworkEntity::toNetwork)
            }

    suspend fun getActiveNetwork(): AppNetwork =
        appNetworkDao
            .getActive()
            .toNetwork()

    suspend fun addInactive(
        genesisHash: String,
        name: String,
        walletProxyUrl: HttpUrl,
        ccdScanFrontendUrl: HttpUrl?,
        notificationsServiceUrl: HttpUrl?,
    ): AppNetwork {
        val newNetwork = AppNetwork(
            name = name,
            genesisHash = genesisHash,
            createdAt = Date(),
            walletProxyUrl = walletProxyUrl,
            notificationsServiceUrl = notificationsServiceUrl,
            ccdScanFrontendUrl = ccdScanFrontendUrl,
            ccdScanBackendUrl = null,
        )
        appNetworkDao.insert(
            AppNetworkEntity(
                network = newNetwork,
                isActive = false,
            )
        )
        return newNetwork
    }

    suspend fun update(
        currentGenesisHash: String,
        newGenesisHash: String,
        name: String,
        walletProxyUrl: HttpUrl,
        ccdScanFrontendUrl: HttpUrl?,
        notificationsServiceUrl: HttpUrl?,
    ): AppNetwork {
        appNetworkDao.update(
            currentGenesisHash = currentGenesisHash,
            newGenesisHash = newGenesisHash,
            name = name,
            walletProxyUrl = walletProxyUrl.toString(),
            ccdScanFrontendUrl = ccdScanFrontendUrl?.toString(),
            notificationsServiceUrl = notificationsServiceUrl?.toString(),
        )
        return getNetworksFlow()
            .first()
            .first { it.genesisHash == newGenesisHash }
    }

    suspend fun activate(
        newActiveNetwork: AppNetwork,
    ) {
        appNetworkDao.activate(
            genesisHash = newActiveNetwork.genesisHash,
        )
    }

    suspend fun delete(
        networkToDelete: AppNetwork,
    ) {
        appNetworkDao.delete(
            genesisHash = networkToDelete.genesisHash,
        )
    }
}
