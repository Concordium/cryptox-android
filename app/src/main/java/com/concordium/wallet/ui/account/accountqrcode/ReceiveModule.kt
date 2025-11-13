package com.concordium.wallet.ui.account.accountqrcode

import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val receiveModule = module {

    viewModel {
        ReceiveViewModel(
            application = androidApplication(),
            mainViewModel = requireNotNull(getOrNull()) {
                "MainViewModel must be provided in parameters"
            }
        )
    }
}