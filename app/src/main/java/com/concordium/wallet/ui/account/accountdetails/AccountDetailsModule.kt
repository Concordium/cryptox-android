package com.concordium.wallet.ui.account.accountdetails

import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountsoverview.AccountBalanceViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val accountDetailsModule = module {
    viewModel { (mainViewModel: MainViewModel) ->
        AccountDetailsViewModel(
            mainViewModel = mainViewModel,
            application = androidApplication()
        )
    }

    viewModel { (accountDetailsViewModel: AccountDetailsViewModel) ->
        AccountBalanceViewModel(
            accountDetailsViewModel = accountDetailsViewModel,
            tokensInteractor = get()
        )
    }
}