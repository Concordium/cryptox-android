package com.concordium.wallet.ui.more.unshielding

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UnshieldingAccountsViewModel(application: Application) : AndroidViewModel(application) {
    private val accountRepository: AccountRepository by lazy {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        AccountRepository(accountDao)
    }
    private val accountUpdater: AccountUpdater by lazy {
        AccountUpdater(application, viewModelScope)
    }

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean> = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>> = _errorLiveData

    private val _listItemsLiveData = MutableLiveData<List<UnshieldingAccountListItem>>()
    val listItemsLiveData: LiveData<List<UnshieldingAccountListItem>> = _listItemsLiveData

    private val _showAuthLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthLiveData: LiveData<Event<Boolean>> = _showAuthLiveData

    private val _goToUnshieldingLiveData = MutableLiveData<Event<Account>>()
    val goToUnshieldingLiveData: LiveData<Event<Account>> = _goToUnshieldingLiveData

    private lateinit var accountAddressToUnshield: String

    init {
        findAccountsMayNeedUnshielding()
    }

    private fun findAccountsMayNeedUnshielding() = viewModelScope.launch(Dispatchers.IO) {
        _waitingLiveData.postValue(true)

        val accountsMayNeedUnshielding = accountRepository.getAllDone()
            .filter(Account::mayNeedUnshielding)

        _listItemsLiveData.postValue(accountsMayNeedUnshielding.map { account ->
            UnshieldingAccountListItem(
                address = account.address,
                name = account.getAccountName(),
                balance = null,
                isUnshielded = false,
            )
        })

        _waitingLiveData.postValue(false)
    }

    fun onUnshieldClicked(item: UnshieldingAccountListItem) {
        accountAddressToUnshield = item.address
        _showAuthLiveData.postValue(Event(true))
    }

    fun onAuthenticated(password: String) = viewModelScope.launch(Dispatchers.IO) {
        val accountToUnshield = accountRepository.findByAddress(accountAddressToUnshield)
            ?: error("Account to unshield $accountAddressToUnshield not found")
        decryptAndUnshield(accountToUnshield, password)
    }

    private suspend fun decryptAndUnshield(account: Account, password: String) {
        _waitingLiveData.postValue(true)

        // Decrypt the private data
        val storageAccountDataEncrypted = account.encryptedAccountData
        if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            _waitingLiveData.postValue(false)
            return
        }
        val decryptedJson = App.appCore.getCurrentAuthenticationManager()
            .decryptInBackground(password, storageAccountDataEncrypted)
        if (decryptedJson != null) {
            val encryptionKey =
                App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
                    .encryptionSecretKey

            accountUpdater.decryptEncryptedAmounts(
                key = encryptionKey,
                account = account,
            )
            accountUpdater.decryptAllUndecryptedAmounts(
                secretPrivateKey = encryptionKey,
            )
            // TODO: Is this really needed?
//            accountUpdater.updateForAccount(account)

            _goToUnshieldingLiveData.postValue(Event(account))
        } else {
            _errorLiveData.postValue(Event(R.string.app_error_encryption))
            _waitingLiveData.postValue(false)
        }
    }
}
