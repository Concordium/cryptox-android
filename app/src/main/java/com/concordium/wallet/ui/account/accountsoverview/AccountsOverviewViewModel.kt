package com.concordium.wallet.ui.account.accountsoverview

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountsOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _listItemsLiveData = MutableLiveData<List<AccountsOverviewListItem>>()
    val listItemsLiveData: LiveData<List<AccountsOverviewListItem>> = _listItemsLiveData

    private val accountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val accountsObserver: Observer<List<AccountWithIdentity>>
    private var updater: CountDownTimer? = null

    init {
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onDone(totalBalances: TotalBalancesData) {
                _waitingLiveData.postValue(false)
            }

            override fun onNewAccountFinalized(accountName: String) {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        if (App.appCore.session.isAccountsBackupPossible()) {
                            App.appCore.session.setAccountsBackedUp(false)
                        }
                    }
                    restartUpdater(BuildConfig.ACCOUNT_UPDATE_FREQUENCY_SEC)
                }
            }

            override fun onError(stringRes: Int) {
                _errorLiveData.postValue(Event(stringRes))
            }
        })
        accountsObserver = Observer { accountsWithIdentity ->
            postListItems(accountsWithIdentity)
        }
        accountRepository.allAccountsWithIdentity.observeForever(accountsObserver)
    }

    override fun onCleared() {
        super.onCleared()
        accountRepository.allAccountsWithIdentity.removeObserver(accountsObserver)
        accountUpdater.dispose()
    }

    fun updateState() = viewModelScope.launch(Dispatchers.IO) {
        // Decide what state to show (visible buttons based on if there is any identities and accounts)
        // Also update all accounts if any exists.
        updateSubmissionStatesAndBalances()
    }

    private fun updateSubmissionStatesAndBalances(notifyWaitingLiveData: Boolean = true) {
        if (notifyWaitingLiveData) {
            _waitingLiveData.postValue(true)
        }
        accountUpdater.updateForAllAccounts()
    }

    fun initiateUpdater() {
        startUpdater()
    }

    fun stopUpdater() {
        updater?.cancel()
        updater = null
    }

    private fun startUpdater(countdownInterval: Long = BuildConfig.ACCOUNT_UPDATE_FREQUENCY_SEC) {
        stopUpdater()
        updater = object : CountDownTimer(Long.MAX_VALUE, countdownInterval * 1000) {
            private var first = true
            override fun onTick(millisUntilFinished: Long) {
                if (first) { // ignore first tick
                    first = false
                    return
                }
                if (isRegularUpdateNeeded()) {
                    updateState()
                    // Log.d("Tick.....")
                }
            }

            override fun onFinish() {
            }
        }.also { it.start() }
    }

    private fun restartUpdater(newInterval: Long) {
        startUpdater(newInterval)
    }

    private fun isRegularUpdateNeeded(): Boolean {
        this.accountRepository.allAccountsWithIdentity.value?.forEach { accountWithIdentity ->
            if (accountWithIdentity.account.transactionStatus != TransactionStatus.FINALIZED) {
                return true
            }
        }
        return false
    }

    private fun postListItems(accountsWithIdentity: List<AccountWithIdentity>) {
        val items = mutableListOf<AccountsOverviewListItem>()
        items.addAll(accountsWithIdentity.map(AccountsOverviewListItem::Account))
        _listItemsLiveData.value = items
    }

    fun activateAccount(address: String) = viewModelScope.launch {
        accountRepository.activate(address)
    }
}
