package com.concordium.wallet.ui.account.accountqrcode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReceiveViewModel(
    application: Application,
    mainViewModel: MainViewModel
) : AndroidViewModel(application) {

    private val _activeAccount = MutableStateFlow<Account?>(null)
    val activeAccount = _activeAccount.asStateFlow()

    init {
        viewModelScope.launch {
            mainViewModel.activeAccount.collect(_activeAccount::emit)
        }
    }

    fun getAccount() = _activeAccount.value
}