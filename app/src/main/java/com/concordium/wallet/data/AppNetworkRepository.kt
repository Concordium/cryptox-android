package com.concordium.wallet.data

import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.data.room.app.AppNetworkDao

class AppNetworkRepository(
    private val appNetworkDao: AppNetworkDao,
) {
    // Predefined networks are added by the Network switch migration.

    suspend fun getActiveNetwork(): AppNetwork =
        appNetworkDao
            .getActive()
            .toNetwork()

    suspend fun activate(
        newActiveNetwork: AppNetwork,
    ) {
        appNetworkDao.activate(
            genesisHash = newActiveNetwork.genesisHash,
        )
    }
}
