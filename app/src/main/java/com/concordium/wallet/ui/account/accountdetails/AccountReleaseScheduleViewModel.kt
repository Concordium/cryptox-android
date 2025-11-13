package com.concordium.wallet.ui.account.accountdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.data.room.Account

class AccountReleaseScheduleViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var account: Account

    fun initialize(account: Account) {
        this.account = account
    }
}
