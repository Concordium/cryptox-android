package com.concordium.wallet.ui.account.earn

import com.concordium.wallet.data.backend.price.tokenPriceModule
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel

import org.koin.dsl.module

val earnModule = module {

    includes(tokenPriceModule)

    viewModel {
        EarnViewModel(
            mainViewModel = requireNotNull(getOrNull()) {
                "MainViewModel must be provided in parameters"
            },
            application = androidApplication()
        )
    }

    viewModel {
        EarnInfoViewModel(
            proxyRepository = ProxyRepository(),
            application = androidApplication()
        )
    }

    viewModel {
        DelegationBakerViewModel(
            tokenPriceRepository = get(),
            application = androidApplication()
        )
    }
}