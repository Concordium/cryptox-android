package com.concordium.wallet.ui.cis2

import com.concordium.wallet.core.tokens.tokensInteractorModule
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val tokensListModule = module {

    includes(
        tokensInteractorModule,
    )

    viewModel {
        TokensListViewModel(
            accountDetailsViewModel = requireNotNull(getOrNull()) {
                "AccountDetailsViewModel must be provided in parameters"
            },
            tokensInteractor = get(),
        )
    }
}
