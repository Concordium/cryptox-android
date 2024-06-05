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
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.util.KeyCreationVersion
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

    private var _stateLiveData = MutableLiveData<State>()
    val stateLiveData: LiveData<State>
        get() = _stateLiveData

    private val zeroAccountBalances = TotalBalancesData(
        BigInteger.ZERO,
        BigInteger.ZERO,
        BigInteger.ZERO,
        BigInteger.ZERO,
        false
    )
    private var _totalBalanceLiveData = MutableLiveData<TotalBalancesData>()
    val totalBalanceLiveData: LiveData<TotalBalancesData>
        get() = _totalBalanceLiveData

    private val _showUnshieldingNoticeLiveData = MutableLiveData<Event<Boolean>>()
    val showUnshieldingNoticeLiveData: LiveData<Event<Boolean>>
        get() = _showUnshieldingNoticeLiveData

    private val _listItemsLiveData = MutableLiveData<List<AccountsOverviewListItem>>()
    val listItemsLiveData: LiveData<List<AccountsOverviewListItem>> = _listItemsLiveData

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val keyCreationVersion: KeyCreationVersion
    private val accountsObserver = Observer<List<AccountWithIdentity>> { accountsWithIdeitity ->
        _listItemsLiveData.value =
            listOf(AccountsOverviewListItem.CcdOnrampBanner) +
                    accountsWithIdeitity.map(AccountsOverviewListItem::Account)
    }

    enum class State {
        NO_IDENTITIES, NO_ACCOUNTS, DEFAULT
    }

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        accountRepository.allAccountsWithIdentity.observeForever(accountsObserver)
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onDone(totalBalances: TotalBalancesData) {
                _waitingLiveData.postValue(false)
                _totalBalanceLiveData.postValue(totalBalances)
            }

            override fun onNewAccountFinalized(accountName: String) {
                viewModelScope.launch(Dispatchers.IO) {
                    if (App.appCore.session.isAccountsBackupPossible()) {
                        App.appCore.session.setAccountsBackedUp(false)
                    }
                }
            }

            override fun onError(stringRes: Int) {
                _errorLiveData.postValue(Event(stringRes))
            }
        })
        keyCreationVersion = KeyCreationVersion(AuthPreferences(application))
    }

    fun initialize() {
        showUnshieldingNoticeIfNeeded()
    }

    override fun onCleared() {
        super.onCleared()
        accountRepository.allAccountsWithIdentity.removeObserver(accountsObserver)
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

    fun checkUsingV1KeyCreation() =
        check(keyCreationVersion.useV1) {
            "Key creation V1 (seed-based) must be used to perform this action"
        }

    private fun showUnshieldingNoticeIfNeeded() = viewModelScope.launch(Dispatchers.IO) {
        // Show the notice once.
        if (App.appCore.session.isUnshieldingNoticeShown()) {
            return@launch
        }

        val anyAccountsMayNeedUnshielding = accountRepository.getAllDone()
            .any(Account::mayNeedUnshielding)

        if (anyAccountsMayNeedUnshielding) {
            _showUnshieldingNoticeLiveData.postValue(Event(true))
        }
    }
}
