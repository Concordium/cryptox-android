package com.concordium.wallet.ui.account.accountdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.Session
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.util.toTransaction
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.onboarding.OnboardingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.math.BigInteger

class AccountDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val session: Session = App.appCore.session

    enum class DialogToShow {
        UNSHIELDING
    }

    lateinit var account: Account

    private val accountRepository = AccountRepository(session.walletStorage.database.accountDao())
    private val transferRepository =
        TransferRepository(session.walletStorage.database.transferDao())

    private val identityRepository =
        IdentityRepository(session.walletStorage.database.identityDao())

    private val recipientRepository =
        RecipientRepository(session.walletStorage.database.recipientDao())

    private lateinit var transactionMappingHelper: TransactionMappingHelper
    private val accountUpdater = AccountUpdater(application, viewModelScope)

    private var nonMergedLocalTransactions: MutableList<Transaction> = ArrayList()

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _newFinalizedAccountFlow = MutableStateFlow("")
    val newFinalizedAccountFlow = _newFinalizedAccountFlow.asStateFlow()

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _finishLiveData = MutableLiveData<Event<Boolean>>()
    val finishLiveData: LiveData<Event<Boolean>>
        get() = _finishLiveData

    private var _totalBalanceLiveData = MutableLiveData<BigInteger>()
    val totalBalanceLiveData: LiveData<BigInteger>
        get() = _totalBalanceLiveData

    private var _accountUpdatedFlow = MutableStateFlow(false)
    val accountUpdatedFlow = _accountUpdatedFlow.asStateFlow()

    private val _showDialogLiveData = MutableLiveData<Event<DialogToShow>>()
    val showDialogLiveData: LiveData<Event<DialogToShow>>
        get() = _showDialogLiveData

    private val _activeAccount = MutableSharedFlow<Account>()
    val activeAccount = _activeAccount.asSharedFlow()

    private val _stateFlow = MutableSharedFlow<OnboardingState>()
    val stateFlow = _stateFlow.asSharedFlow()

    private val _identityFlow = MutableSharedFlow<Identity>()
    val identityFlow = _identityFlow.asSharedFlow()

    private val _fileWalletMigrationVisible = MutableStateFlow(false)
    val fileWalletMigrationVisible = _fileWalletMigrationVisible.asStateFlow()

    private val _hasPendingDelegationTransactions = MutableStateFlow(false)
    val hasPendingDelegationTransactions = _hasPendingDelegationTransactions.asStateFlow()

    private val _hasPendingBakingTransactions = MutableStateFlow(false)
    val hasPendingBakingTransactions = _hasPendingBakingTransactions.asStateFlow()

    private var updaterJob: Job? = null

    val isCreationLimitedForFileWallet: Boolean
        get() = App.appCore.session.activeWallet.type == AppWallet.Type.FILE

    init {
        initializeAccountUpdater()
        _fileWalletMigrationVisible.tryEmit(
            App.appCore.session.activeWallet.type == AppWallet.Type.FILE
        )
    }

    fun setShowOnrampBanner(show: Boolean) {
        return App.appCore.session.walletStorage.setupPreferences.setShowOnrampBanner(show)
    }

    fun isShowOnrampBanner(): Boolean {
        return App.appCore.session.walletStorage.setupPreferences.getShowOnrampBanner()
    }

    private fun getActiveAccount() = viewModelScope.launch {
        val acc = accountRepository.getActive()
        acc?.let {
            account = it
            _activeAccount.emit(it)
            _totalBalanceLiveData.postValue(it.balance)

            if (account.transactionStatus == TransactionStatus.COMMITTED ||
                account.transactionStatus == TransactionStatus.RECEIVED) {
                restartUpdater(BuildConfig.FAST_ACCOUNT_UPDATE_FREQUENCY_SEC)
            }
        }
    }

    fun updateAccount() {
        getActiveAccount()
    }

    override fun onCleared() {
        super.onCleared()
        accountUpdater.dispose()
    }

    fun deleteAccountAndFinish() = viewModelScope.launch {
        accountRepository.delete(account)
        _finishLiveData.value = Event(true)
    }

    fun populateTransferList(notifyWaitingLiveData: Boolean = true) {
        if (::account.isInitialized) {
            if (account.transactionStatus == TransactionStatus.FINALIZED) {
                if (notifyWaitingLiveData) {
                    _waitingLiveData.value = true
                }
                viewModelScope.launch {
                    accountUpdater.updateForAccount(account)
                    val type =
                        if (account.delegation != null) ProxyRepository.UPDATE_DELEGATION else ProxyRepository.REGISTER_BAKER
                    EventBus.getDefault().post(
                        BakerDelegationData(
                            account,
                            isTransactionInProgress = hasPendingDelegationTransactions.value || hasPendingBakingTransactions.value,
                            type = type
                        )
                    )
                }
            } else {
                _totalBalanceLiveData.postValue(BigInteger.ZERO)
            }
        }
    }

    private fun initializeAccountUpdater() {
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onDone(totalBalances: TotalBalancesData) {
                getLocalTransfers()
                viewModelScope.launch {
                    updateAccountFromRepository()
                    if (::account.isInitialized) {
                        _totalBalanceLiveData.value = account.balance
                    }
                    _accountUpdatedFlow.emit(true)
                }
            }

            override fun onNewAccountFinalized(accountName: String) { }

            override fun onError(stringRes: Int) {
                _errorLiveData.value = Event(stringRes)
            }
        })
    }

    private fun updateSubmissionStatesAndBalances(notifyWaitingLiveData: Boolean = true) {
        if (notifyWaitingLiveData) {
            _waitingLiveData.postValue(true)
        }
        accountUpdater.updateForAllAccounts()
    }

    private suspend fun updateAccountFromRepository() {
        if (::account.isInitialized) {
            accountRepository.findById(account.id)?.let { accountCandidate ->
                account = accountCandidate
            }
        }
    }

    private fun getLocalTransfers() {
        viewModelScope.launch {
            _hasPendingDelegationTransactions.emit(false)
            _hasPendingBakingTransactions.emit(false)
            val recipientList = recipientRepository.getAll()
            transactionMappingHelper = TransactionMappingHelper(activeAccount.first(), recipientList)
            val transferList = transferRepository.getAllByAccountId(activeAccount.first().id)
            for (transfer in transferList) {
                if (transfer.transactionType == TransactionType.LOCAL_DELEGATION)
                    _hasPendingDelegationTransactions.emit(true)
                if (transfer.transactionType == TransactionType.LOCAL_BAKER)
                    _hasPendingBakingTransactions.emit(true)
                val transaction = transfer.toTransaction()
                transactionMappingHelper.addTitlesToTransaction(
                    transaction,
                    transfer,
                    getApplication()
                )
                nonMergedLocalTransactions.add(transaction)
            }
        }
    }

    fun updateState(notifyWaitingLiveData: Boolean = true) = viewModelScope.launch(Dispatchers.IO) {
        // Decide what state to show (visible buttons based on if there is any identities and accounts)
        // Also update all accounts (and set the overall balance) if any exists.

        if (App.appCore.session.activeWallet.type == AppWallet.Type.FILE) {
            postState(OnboardingState.DONE, notifyWaitingLiveData = notifyWaitingLiveData)
            getActiveAccount()
            updateSubmissionStatesAndBalances(notifyWaitingLiveData)
            return@launch
        }

        when {
            !App.appCore.session.walletStorage.setupPreferences.hasEncryptedSeed() ->
                postState(OnboardingState.SAVE_PHRASE)

            identityRepository.getCount() == 0 ->
                postState(
                    OnboardingState.VERIFY_IDENTITY
                )

            else ->
                updateIdentityStatus()
        }
    }

    private suspend fun postState(
        state: OnboardingState,
        notifyWaitingLiveData: Boolean = true,
    ) {
        _stateFlow.emit(state)
        if (notifyWaitingLiveData) {
            _waitingLiveData.postValue(false)
        }
    }

    private suspend fun updateIdentityStatus() {
        val doneCount = identityRepository.getAllDone().size
        when {
            doneCount > 0 -> handleDoneIdentities()
            identityRepository.getAllPending().isNotEmpty() -> postState(
                OnboardingState.IDENTITY_IN_PROGRESS
            )

            else -> postState(
                OnboardingState.IDENTITY_UNSUCCESSFUL
            )
        }
    }

    private suspend fun handleDoneIdentities() {
        val accountCount = accountRepository.getCount()
        if (accountCount == 0) {
            val identity = identityRepository.getAllDone().first()
            _identityFlow.emit(identity)
            postState(OnboardingState.CREATE_ACCOUNT)
        } else {
            handleAccountCreation()
        }
    }

    private suspend fun handleAccountCreation() {
        val allAccounts = accountRepository.getAll()
        if (allAccounts.any { it.transactionStatus == TransactionStatus.FINALIZED }) {
            App.appCore.session.walletStorage.setupPreferences.setHasCompletedOnboarding(true)
            getActiveAccount()
            postState(OnboardingState.DONE)
            showSingleDialogIfNeeded()
        } else {
            postState(state = OnboardingState.FINALIZING_ACCOUNT)
            _waitingLiveData.postValue(false)
            restartUpdater(BuildConfig.FAST_ACCOUNT_UPDATE_FREQUENCY_SEC)
        }
        updateSubmissionStatesAndBalances(false)
    }

    private fun showSingleDialogIfNeeded() = viewModelScope.launch(Dispatchers.IO) {
        val dialogsToShow = linkedSetOf<DialogToShow>()

        // Show unshielding notice if never shown and some accounts may need unshielding.
        if (!App.appCore.session.isUnshieldingNoticeShown()
            && accountRepository.getAllDone().any(Account::mayNeedUnshielding)
        ) {
            dialogsToShow += DialogToShow.UNSHIELDING
        }
        // Show a single dialog if needed.
        if (dialogsToShow.isNotEmpty()) {
            _showDialogLiveData.postValue(Event(dialogsToShow.first()))
        }
    }

    fun initiateFrequentUpdater() {
        startUpdater()
    }

    fun stopFrequentUpdater() {
        updaterJob?.cancel()
        updaterJob = null
    }

    private fun restartUpdater(newInterval: Long) {
        startUpdater(newInterval)
    }

    private fun startUpdater(interval: Long = BuildConfig.ACCOUNT_UPDATE_FREQUENCY_SEC) {
        stopFrequentUpdater()

        updaterJob = viewModelScope.launch(Dispatchers.IO) {
            delay(interval * 1000) // Initial delay
            while (isActive) {
                updateState()
                populateTransferList(false)
                delay(interval * 1000)
            }
        }
    }


    fun hasShownInitialAnimation(): Boolean {
        return App.appCore.session.walletStorage.setupPreferences.getHasShownInitialAnimation()
    }

    fun setHasShownInitialAnimation() {
        return App.appCore.session.walletStorage.setupPreferences.setHasShownInitialAnimation(true)
    }
}
