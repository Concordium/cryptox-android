package com.concordium.wallet.core.tokens

import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.PLTRepository
import com.concordium.wallet.data.backend.price.TokenPriceRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import org.koin.dsl.module

val tokensInteractorModule = module {
    single {
        TokensInteractor(
            proxyRepository = ProxyRepository(),
            contractTokensRepository = ContractTokensRepository(
                App.appCore.session.walletStorage.database.contractTokenDao()
            ),
            pltRepository = PLTRepository(
                App.appCore.session.walletStorage.database.protocolLevelTokenDao()
            ),
            accountRepository = AccountRepository(
                App.appCore.session.walletStorage.database.accountDao()
            ),
            tokenPriceRepository = TokenPriceRepository(ProxyRepository()),
            loadTokensBalancesUseCase = LoadTokensBalancesUseCase()
        )
    }
}