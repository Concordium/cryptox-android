package com.concordium.wallet.ui.account.accountdetails

import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val accountDetailsModule = module {
    viewModel {
        AccountDetailsViewModel(
            mainViewModel =requireNotNull(getOrNull()) {
                "MainViewModel must be provided in parameters"
            },
            application = androidApplication()
        )
    }
}