package com.concordium.wallet.ui.account.accountsoverview

import com.concordium.wallet.core.tokens.tokensInteractorModule
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val accountBalanceModule = module {
    includes(
        tokensInteractorModule,
    )

    viewModel { (accountDetailsViewModel: AccountDetailsViewModel) ->
        AccountBalanceViewModel(
            accountDetailsViewModel = accountDetailsViewModel,
            tokensInteractor = get()
        )
    }
}