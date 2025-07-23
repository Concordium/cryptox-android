package com.concordium.wallet.ui.cis2

import com.concordium.wallet.data.backend.price.tokenPriceModule
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val sendTokenModule = module {

    includes(
        tokenPriceModule,
    )

    viewModel {
        SendTokenViewModel(
            sendTokenData = requireNotNull(getOrNull()) {
                "SendTokenData must be provided in parameters"
            },
            application = androidApplication(),
        )
    }
}
