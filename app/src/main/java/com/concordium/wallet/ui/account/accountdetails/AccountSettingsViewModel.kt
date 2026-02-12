package com.concordium.wallet.ui.account.accountdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.room.Account
import kotlinx.coroutines.launch

class AccountSettingsViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var account: Account
    var identityName: String = ""
    val canExportTransactionLogs: Boolean
        get() = App.appCore.session.network.ccdScanBackendUrl != null

    val accountUpdated: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    fun initialize(account: Account) = initData(account)

    fun changeAccountName(name: String) {
        val accountRepository =
            AccountRepository(App.appCore.session.walletStorage.database.accountDao())
        val recipientRepository =
            RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())
        viewModelScope.launch {
            accountUpdated.postValue(false)
            account.name = name
            accountRepository.update(account)
            recipientRepository.getRecipientByAddress(account.address)?.let { recipient ->
                recipient.name = name
                recipientRepository.update(recipient)
            }
            accountUpdated.postValue(true)
        }
    }

    private fun initData(account: Account) {
        this.account = account

        val identityRepository = IdentityRepository(
            App.appCore.session.walletStorage.database.identityDao()
        )
        viewModelScope.launch {
            identityRepository.findById(account.identityId)?.let {
                identityName = it.name
                accountUpdated.postValue(true)
            }
        }
    }
}
