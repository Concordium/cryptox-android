package com.concordium.wallet.ui.account.newaccountconfirmed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import kotlinx.coroutines.launch

class NewAccountConfirmedViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    lateinit var account: Account

    lateinit var accountWithIdentityLiveData: LiveData<AccountWithIdentity>

    fun initialize(account: Account) {
        this.account = account
        accountWithIdentityLiveData = accountRepository.getByIdWithIdentityAsLiveData(account.id)
    }

    fun updateState() {
        _waitingLiveData.value = true
        viewModelScope.launch {
            _waitingLiveData.value = false
        }
    }
}
