package com.concordium.wallet.data.backend.devnet

import com.concordium.wallet.App
import com.concordium.wallet.data.model.PLTInfo

class DevnetRepository {

    private val backend = App.appCore.getDevnetBackend()

    suspend fun getAccountBalanceSuspended(
        accountAddress: String = "4GbHu8Ynnt1hc2PGhRAiwGzkXYBxnSCNJEB9dcnGEJPehRw3oo"
    ) = backend.accountBalanceSuspended(accountAddress)

    /**
     * @return a list of plt token infos.
     */
    suspend fun getPLTTokens(): List<PLTInfo> = backend.pltTokens()

    /**
     * @return token info and decoded module state
     */
    suspend fun getPLTTokenById(tokenId: String): PLTInfo = backend.getPLTTokenById(tokenId)
}