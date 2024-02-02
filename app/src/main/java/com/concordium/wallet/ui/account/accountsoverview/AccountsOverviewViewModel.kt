package com.concordium.wallet.ui.account.accountsoverview

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger

class AccountsOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _newFinalizedAccountLiveData = MutableLiveData<String>()
    val newFinalizedAccountLiveData: LiveData<String>
        get() = _newFinalizedAccountLiveData

    private var _stateLiveData = MutableLiveData<State>()
    val stateLiveData: LiveData<State>
        get() = _stateLiveData

    private val zeroAccountBalances = TotalBalancesData(
        BigInteger.ZERO,
        BigInteger.ZERO,
        BigInteger.ZERO,
        BigInteger.ZERO, false
    )
    private var _totalBalanceLiveData = MutableLiveData<TotalBalancesData>()
    val totalBalanceLiveData: LiveData<TotalBalancesData>
        get() = _totalBalanceLiveData

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val accountUpdater = AccountUpdater(application, viewModelScope)
    val accountListLiveData: LiveData<List<AccountWithIdentity>>

    enum class State {
        NO_IDENTITIES, NO_ACCOUNTS, DEFAULT
    }

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        accountListLiveData = accountRepository.allAccountsWithIdentity
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onDone(totalBalances: TotalBalancesData) {
                _waitingLiveData.postValue(false)
                _totalBalanceLiveData.postValue(totalBalances)
            }

            override fun onNewAccountFinalized(accountName: String) {
                viewModelScope.launch(Dispatchers.IO) {
                    if (App.appCore.session.isAccountsBackupPossible()) {
                        _newFinalizedAccountLiveData.postValue(accountName)
                        App.appCore.session.setAccountsBackedUp(false)
                    }
                }
            }

            override fun onError(stringRes: Int) {
                _errorLiveData.postValue(Event(stringRes))
            }
        })
    }

    fun initialize() {
    }

    override fun onCleared() {
        super.onCleared()
        accountUpdater.dispose()
    }

    fun updateState(notifyWaitingLiveData: Boolean = true) {
        // Decide what state to show (visible buttons based on if there is any identities and accounts)
        // Also update all accounts (and set the overall balance) if any exists.
        viewModelScope.launch(Dispatchers.IO) {
            val identityCount = identityRepository.getNonFailedCount()
            if (identityCount == 0) {
                _stateLiveData.postValue(State.NO_IDENTITIES)
                // Set balance, because we know it will be 0
                _totalBalanceLiveData.postValue(zeroAccountBalances)
                if (notifyWaitingLiveData) {
                    _waitingLiveData.postValue(false)
                }
            } else {
                val accountCount = accountRepository.getCount()
                if (accountCount == 0) {
                    _stateLiveData.postValue(State.NO_ACCOUNTS)
                    // Set balance, because we know it will be 0
                    _totalBalanceLiveData.postValue(zeroAccountBalances)
                    if (notifyWaitingLiveData) {
                        _waitingLiveData.postValue(false)
                    }
                } else {
                    _stateLiveData.postValue(State.DEFAULT)
                    if (notifyWaitingLiveData) {
                        _waitingLiveData.postValue(false)
                    }
                    updateSubmissionStatesAndBalances(notifyWaitingLiveData)
                }
            }
        }
    }

    private fun updateSubmissionStatesAndBalances(notifyWaitingLiveData: Boolean = true) {
        if (notifyWaitingLiveData) {
            _waitingLiveData.postValue(true)
        }
        accountUpdater.updateForAllAccounts()
    }

    fun initiateFrequentUpdater() {
        updater.cancel()
        updater.start()
    }

    fun stopFrequentUpdater() {
        updater.cancel()
    }

    var updater =
        object : CountDownTimer(Long.MAX_VALUE, BuildConfig.ACCOUNT_UPDATE_FREQUENCY_SEC * 1000) {
            private var first = true
            override fun onTick(millisUntilFinished: Long) {
                if (first) { // ignore first tick
                    first = false
                    return
                }
                if (isRegularUpdateNeeded()) {
                    updateState(false)
                    // Log.d("Tick.....")
                }
            }

            override fun onFinish() {
            }
        }

    private fun isRegularUpdateNeeded(): Boolean {
        this.accountRepository.allAccountsWithIdentity.value?.forEach { accountWithIdentity ->
            if (accountWithIdentity.account.transactionStatus != TransactionStatus.FINALIZED) {
                return true
            }
        }
        return false
    }
}
