package com.concordium.wallet.ui.onramp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import kotlinx.coroutines.launch

class CcdOnrampAccountsViewModel(application: Application) : AndroidViewModel(application) {
    private val accountRepository: AccountRepository by lazy {
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    }

    private val _listItemsLiveData = MutableLiveData<List<CcdOnrampAccountListItem>>()
    val listItemsLiveData: LiveData<List<CcdOnrampAccountListItem>> = _listItemsLiveData

    init {
        App.appCore.tracker.homeOnRampScreen()
        postItems()
    }

    private fun postItems() = viewModelScope.launch {
        val accountsWithIdentity = accountRepository.getAllDoneWithIdentity()
        _listItemsLiveData.postValue(
            accountsWithIdentity.mapIndexed { i, accountWithIdentity ->
                CcdOnrampAccountListItem(
                    source = accountWithIdentity,
                    isDividerVisible = i < accountsWithIdentity.size - 1
                )
            }
        )
    }
}
