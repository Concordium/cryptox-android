package com.concordium.wallet.ui.onramp

import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val onrampModule = module {
    viewModel {
        CcdOnrampSitesViewModel(
            mainViewModel = requireNotNull(getOrNull()) {
                "MainViewModel must be provided in parameters"
            },
            application = androidApplication()
        )
    }
}