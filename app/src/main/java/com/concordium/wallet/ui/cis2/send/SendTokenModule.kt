package com.concordium.wallet.ui.cis2.send

import com.concordium.wallet.core.tokens.tokensInteractorModule
import com.concordium.wallet.data.backend.price.tokenPriceModule
import com.concordium.wallet.data.backend.repository.ProxyRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sendTokenModule = module {

    includes(
        tokenPriceModule,
        tokensInteractorModule,
    )

    viewModel {
        SendTokenViewModel(
            tokenPriceRepository = get(),
            mainViewModel = requireNotNull(getOrNull()) {
                "MainViewModel must be provided in parameters"
            },
            application = androidApplication(),
        )
    }

    viewModel {
        SelectTokenViewModel(
            accountAddress = requireNotNull(getOrNull()){
                "Account address must be provided in parameters"
            },
            tokensInteractor = get(),
        )
    }

    viewModel {
        SendTokenReceiptViewModel(
            sendTokenData = requireNotNull(getOrNull()) {
                "SendTokenData must be provided in parameters"
            },
            proxyRepository = ProxyRepository(),
            application = androidApplication(),
        )
    }
}
