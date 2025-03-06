package com.concordium.wallet.data.backend.price

import com.concordium.wallet.data.backend.repository.ProxyRepository
import org.koin.dsl.module

val tokenPriceModule = module {
    single {
        TokenPriceRepository(
            proxyRepository = ProxyRepository(),
        )
    }
}
