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
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.core.notifications.UpdateNotificationsSubscriptionUseCase
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountWithIdentity
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.onboarding.OnboardingState
import com.concordium.wallet.ui.onramp.CcdOnrampSiteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger

class AccountsOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _stateFlow = MutableSharedFlow<OnboardingState>()
    val stateFlow = _stateFlow.asSharedFlow()

    private val _identityFlow = MutableSharedFlow<Identity>()
    val identityFlow = _identityFlow.asSharedFlow()

    private val _onrampActive = MutableStateFlow(false)
    val onrampActive = _onrampActive.asStateFlow()

    private val _fileWalletMigrationVisible = MutableStateFlow(false)
    val fileWalletMigrationVisible = _fileWalletMigrationVisible.asSharedFlow()

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

    private val identityRepository =
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    private val accountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val ccdOnrampSiteRepository: CcdOnrampSiteRepository
    private val accountsObserver: Observer<List<AccountWithIdentity>>
    private val walletNotificationsPreferences =
        App.appCore.session.walletStorage.notificationsPreferences
    private val updateNotificationsSubscriptionUseCase by lazy(::UpdateNotificationsSubscriptionUseCase)
    val isCreationLimitedForFileWallet: Boolean
        get() = App.appCore.session.activeWallet.type == AppWallet.Type.FILE

    enum class DialogToShow {
        UNSHIELDING,
        NOTIFICATIONS_PERMISSION,
        ;
    }

    init {
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
        ccdOnrampSiteRepository = CcdOnrampSiteRepository()
        accountsObserver = Observer { accountsWithIdentity ->
            postListItems(accountsWithIdentity)
        }
        accountRepository.allAccountsWithIdentity.observeForever(accountsObserver)
        _fileWalletMigrationVisible.tryEmit(App.appCore.session.activeWallet.type == AppWallet.Type.FILE)
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

    fun updateState(notifyWaitingLiveData: Boolean = true) = viewModelScope.launch(Dispatchers.IO) {
        // Decide what state to show (visible buttons based on if there is any identities and accounts)
        // Also update all accounts (and set the overall balance) if any exists.

        if (App.appCore.session.activeWallet.type == AppWallet.Type.FILE) {
            postState(OnboardingState.DONE, notifyWaitingLiveData = notifyWaitingLiveData)
            updateSubmissionStatesAndBalances()
            return@launch
        }

        when {
            !App.appCore.session.walletStorage.setupPreferences.hasEncryptedSeed() ->
                postState(OnboardingState.SAVE_PHRASE, zeroAccountBalances, notifyWaitingLiveData)

            identityRepository.getCount() == 0 ->
                postState(
                    OnboardingState.VERIFY_IDENTITY,
                    zeroAccountBalances,
                    notifyWaitingLiveData
                )

            else ->
                updateIdentityStatus(notifyWaitingLiveData)
        }
    }

    private suspend fun postState(
        state: OnboardingState,
        balances: TotalBalancesData = zeroAccountBalances,
        notifyWaitingLiveData: Boolean = true,
    ) {
        _stateFlow.emit(state)
        _totalBalanceLiveData.postValue(balances)
        if (notifyWaitingLiveData) {
            _waitingLiveData.postValue(false)
        }
    }

    private suspend fun updateIdentityStatus(notifyWaitingLiveData: Boolean) {
        val doneCount = identityRepository.getAllDone().size
        when {
            doneCount > 0 -> handleDoneIdentities(notifyWaitingLiveData)
            identityRepository.getAllPending().isNotEmpty() -> postState(
                OnboardingState.IDENTITY_IN_PROGRESS,
                zeroAccountBalances,
                notifyWaitingLiveData
            )

            else -> postState(
                OnboardingState.IDENTITY_UNSUCCESSFUL,
                zeroAccountBalances,
                notifyWaitingLiveData
            )
        }
    }

    private suspend fun handleDoneIdentities(notifyWaitingLiveData: Boolean) {
        val accountCount = accountRepository.getCount()
        if (accountCount == 0) {
            val identity = identityRepository.getAllDone().first()
            _identityFlow.emit(identity)
            postState(OnboardingState.CREATE_ACCOUNT, zeroAccountBalances, notifyWaitingLiveData)
        } else {
            App.appCore.session.walletStorage.setupPreferences.setHasCompletedOnboarding(true)
            postState(OnboardingState.DONE, notifyWaitingLiveData = notifyWaitingLiveData)
            updateSubmissionStatesAndBalances()
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

    fun hasShownInitialAnimation(): Boolean {
        return App.appCore.session.walletStorage.setupPreferences.getHasShownInitialAnimation()
    }

    fun setHasShownInitialAnimation() {
        return App.appCore.session.walletStorage.setupPreferences.setHasShownInitialAnimation(true)
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

    fun checkNotFileWallet() =
        check(App.appCore.session.activeWallet.type != AppWallet.Type.FILE) {
            "File wallet can't used to perform this action"
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

        // Only set the onramp banner active if has sites and there are finalized accounts.
        if (ccdOnrampSiteRepository.hasSites
            && accountsWithIdentity.any { it.account.transactionStatus == TransactionStatus.FINALIZED }
        ) {
            viewModelScope.launch {
                _onrampActive.emit(true)
            }
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
