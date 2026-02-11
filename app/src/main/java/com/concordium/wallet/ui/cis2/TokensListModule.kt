package com.concordium.wallet.ui.cis2

import com.concordium.wallet.core.tokens.tokensInteractorModule
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tokensListModule = module {

    includes(
        tokensInteractorModule,
    )

    viewModel { (accountDetailsViewModel: AccountDetailsViewModel) ->
        TokensListViewModel(
            accountDetailsViewModel = accountDetailsViewModel,
            tokensInteractor = get(),
        )
    }
}
