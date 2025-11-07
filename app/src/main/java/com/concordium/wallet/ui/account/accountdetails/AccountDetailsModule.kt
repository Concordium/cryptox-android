package com.concordium.wallet.ui.account.accountdetails

import com.concordium.wallet.ui.MainViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val accountDetailsModule = module {
    viewModel { (mainViewModel: MainViewModel) ->
        AccountDetailsViewModel(
            mainViewModel = mainViewModel,
            application = androidApplication()
        )
    }
}