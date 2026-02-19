package com.concordium.wallet.data

import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.data.room.app.AppNetworkDao
import com.concordium.wallet.data.room.app.AppNetworkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    suspend fun addAndActivate(
        newNetwork: AppNetwork,
    ) {
        appNetworkDao.insertAndActivate(
            network = AppNetworkEntity(
                network = newNetwork,
                isActive = true,
            ),
        )
    }

    suspend fun activate(
        newActiveNetwork: AppNetwork,
    ) {
        appNetworkDao.activate(
            genesisHash = newActiveNetwork.genesisHash,
        )
    }
}
