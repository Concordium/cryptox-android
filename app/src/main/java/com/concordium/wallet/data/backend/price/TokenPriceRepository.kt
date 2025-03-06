package com.concordium.wallet.data.backend.price

import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.SimpleFraction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenPriceRepository(
    private val proxyRepository: ProxyRepository,
) {

    private var cachedEurPerMicroCcd: Pair<SimpleFraction, Long>? = null
    private val eurPerMicroCcdMutex = Mutex()

    suspend fun getEurPerMicroCcd(): Result<SimpleFraction> = eurPerMicroCcdMutex.withLock {
        runCatching {
            cachedEurPerMicroCcd
                ?.takeIf { System.currentTimeMillis() - it.second < CACHE_TTL_MS }
                ?.first
                ?: getFreshEurPerMicroCcd().also {
                    synchronized(this@TokenPriceRepository) {
                        cachedEurPerMicroCcd = it to System.currentTimeMillis()
                    }
                }
        }
    }

    private suspend fun getFreshEurPerMicroCcd(): SimpleFraction =
        proxyRepository
            .getChainParameters()
            .microGtuPerEuro
            .let { microGtuPerEuro ->
                // Reverse the fraction.
                SimpleFraction(
                    numerator = microGtuPerEuro.denominator,
                    denominator = microGtuPerEuro.numerator,
                )
            }

    private companion object {
        private const val CACHE_TTL_MS = 60 * 60 * 1000
    }
}
