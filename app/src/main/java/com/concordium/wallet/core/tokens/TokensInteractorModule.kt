package com.concordium.wallet.core.tokens

import com.concordium.wallet.data.backend.price.tokenPriceModule
import com.concordium.wallet.data.backend.repository.ProxyRepository
import org.koin.dsl.module

val tokensInteractorModule = module {

    includes(
        tokenPriceModule,
    )

    single {
        TokensInteractor(
            proxyRepository = ProxyRepository(),
            tokenPriceRepository = get(),
            loadTokensBalancesUseCase = LoadTokensBalancesUseCase(),
            loadCIS2TokensUseCase = LoadCIS2TokensUseCase(),
            loadCIS2TokensMetadataUseCase = LoadCIS2TokensMetadataUseCase(),
        )
    }
}
