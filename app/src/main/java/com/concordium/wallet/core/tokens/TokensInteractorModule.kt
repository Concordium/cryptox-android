package com.concordium.wallet.core.tokens

import com.concordium.wallet.App
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.PLTRepository
import org.koin.dsl.module

val tokensInteractorModule = module {
    single {
        TokensInteractor(
            contractTokensRepository = ContractTokensRepository(
                App.appCore.session.walletStorage.database.contractTokenDao()
            ),
            pltRepository = PLTRepository(
                App.appCore.session.walletStorage.database.protocolLevelTokenDao()
            )
        )
    }
}