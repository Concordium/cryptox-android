package com.concordium.wallet.ui.account.accountdetails.transfers

import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val transfersModule = module {

    viewModel {
        TransfersViewModel(
            application = androidApplication(),
            mainViewModel = requireNotNull(getOrNull()) {
                "MainViewModel must be provided in parameters"
            }
        )
    }
}