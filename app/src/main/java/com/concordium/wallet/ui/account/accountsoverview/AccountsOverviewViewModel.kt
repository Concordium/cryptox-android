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
import com.concordium.wallet.core.notifications.UpdateNotificationsSubscriptionUseCase
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.preferences.WalletSetupPreferences
import com.concordium.wallet.data.preferences.WalletNotificationsPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.onramp.CcdOnrampSiteRepository
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

    private val _showDialogLiveData = MutableLiveData<Event<DialogToShow>>()
    val showDialogLiveData: LiveData<Event<DialogToShow>>
        get() = _showDialogLiveData

    private val _listItemsLiveData = MutableLiveData<List<AccountsOverviewListItem>>()
    val listItemsLiveData: LiveData<List<AccountsOverviewListItem>> = _listItemsLiveData

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val keyCreationVersion: KeyCreationVersion
    private val ccdOnrampSiteRepository: CcdOnrampSiteRepository
    private val accountsObserver: Observer<List<AccountWithIdentity>>
    private val walletNotificationsPreferences: WalletNotificationsPreferences

    private val updateNotificationsSubscriptionUseCase by lazy {
        UpdateNotificationsSubscriptionUseCase(application)
    }

    enum class State {
        NO_IDENTITIES,
        NO_ACCOUNTS,
        DEFAULT,
        ;
    }

    enum class DialogToShow {
        UNSHIELDING,
        NOTIFICATIONS_PERMISSION,
        ;
    }

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
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
        keyCreationVersion = KeyCreationVersion(WalletSetupPreferences(application))
        ccdOnrampSiteRepository = CcdOnrampSiteRepository()
        accountsObserver = Observer { accountsWithIdentity ->
            postListItems(accountsWithIdentity)
        }
        accountRepository.allAccountsWithIdentity.observeForever(accountsObserver)
        walletNotificationsPreferences = WalletNotificationsPreferences(application)
        updateNotificationSubscription()
    }

    fun initialize() {
        showSingleDialogIfNeeded()
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

    private var updater =
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

    private fun showSingleDialogIfNeeded() = viewModelScope.launch(Dispatchers.IO) {
        val dialogsToShow = linkedSetOf<DialogToShow>()

        // Show unshielding notice if never shown and some accounts may need unshielding.
        if (!App.appCore.session.isUnshieldingNoticeShown()
            && accountRepository.getAllDone().any(Account::mayNeedUnshielding)
        ) {
            dialogsToShow += DialogToShow.UNSHIELDING
        }

        // Show notifications permission if never shown.
        if (!walletNotificationsPreferences.hasEverShownPermissionDialog) {
            dialogsToShow += DialogToShow.NOTIFICATIONS_PERMISSION
        }

        // Show a single dialog if needed.
        if (dialogsToShow.isNotEmpty()) {
            _showDialogLiveData.postValue(Event(dialogsToShow.first()))
        }
    }

    private fun postListItems(accountsWithIdentity: List<AccountWithIdentity>) {
        val items = mutableListOf<AccountsOverviewListItem>()

        // Only show the onramp banner if has sites and there are finalized accounts.
        if (ccdOnrampSiteRepository.hasSites
            && accountsWithIdentity.any { it.account.transactionStatus == TransactionStatus.FINALIZED }
        ) {
            items.add(AccountsOverviewListItem.CcdOnrampBanner)
        }

        items.addAll(accountsWithIdentity.map(AccountsOverviewListItem::Account))

        _listItemsLiveData.value = items
    }

    private fun updateNotificationSubscription() {
        viewModelScope.launch(Dispatchers.IO) {
            updateNotificationsSubscriptionUseCase()
        }
    }
}
