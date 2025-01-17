package com.concordium.wallet.ui.account.accountdetails

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.Session
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.model.RemoteTransaction
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.util.toTransaction
import com.concordium.wallet.ui.account.accountdetails.transfers.AdapterItem
import com.concordium.wallet.ui.account.accountdetails.transfers.HeaderItem
import com.concordium.wallet.ui.account.accountdetails.transfers.TransactionItem
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.math.BigInteger
import java.util.Date

class AccountDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val session: Session = App.appCore.session

    lateinit var account: Account
    var hasPendingDelegationTransactions: Boolean = false
    var hasPendingBakingTransactions: Boolean = false

    private val proxyRepository = ProxyRepository()
    private val accountRepository = AccountRepository(session.walletStorage.database.accountDao())
    private val transferRepository = TransferRepository(session.walletStorage.database.transferDao())
    private val identityRepository = IdentityRepository(session.walletStorage.database.identityDao())
    private val recipientRepository = RecipientRepository(session.walletStorage.database.recipientDao())

    private lateinit var transactionMappingHelper: TransactionMappingHelper
    private val accountUpdater = AccountUpdater(application, viewModelScope)

    // Transaction state
    private var nonMergedLocalTransactions: MutableList<Transaction> = ArrayList()
    var hasMoreRemoteTransactionsToLoad = true
        private set
    private var lastRemoteTransaction: RemoteTransaction? = null
    private var isLoadingTransactions = false
    var allowScrollToLoadMore = true
    private var lastHeaderDate: Date? = null

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _newFinalizedAccountLiveData = MutableLiveData<String>()

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _finishLiveData = MutableLiveData<Event<Boolean>>()
    val finishLiveData: LiveData<Event<Boolean>>
        get() = _finishLiveData

    private val _showGTUDropLiveData = MutableLiveData<Boolean>()
    val showGTUDropLiveData: LiveData<Boolean>
        get() = _showGTUDropLiveData

    private val _showPadLockLiveData = MutableLiveData<Boolean>()
    val showPadLockLiveData: LiveData<Boolean>
        get() = _showPadLockLiveData

    private var _transferListLiveData = MutableLiveData<List<AdapterItem>?>()
    val transferListLiveData: MutableLiveData<List<AdapterItem>?>
        get() = _transferListLiveData

    private var _identityLiveData = MutableLiveData<Identity?>()
    val identityLiveData: MutableLiveData<Identity?>
        get() = _identityLiveData

    private var _totalBalanceLiveData = MutableLiveData<BigInteger>()
    val totalBalanceLiveData: LiveData<BigInteger>
        get() = _totalBalanceLiveData

    private var _accountUpdatedLiveData = MutableLiveData<Boolean>()
    val accountUpdatedLiveData: LiveData<Boolean>
        get() = _accountUpdatedLiveData

    init {
        initializeAccountUpdater()

        _transferListLiveData.observeForever {
            updateGTUDropState()
        }

        viewModelScope.launch {
            account = accountRepository.getActive()
            _totalBalanceLiveData.postValue(account.balance)
            getIdentityProvider()
            Log.d("Account address: ${account.address}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        accountUpdater.dispose()
    }

    fun deleteAccountAndFinish() = viewModelScope.launch {
        accountRepository.delete(account)
        _finishLiveData.value = Event(true)
    }

    private fun getIdentityProvider() {
        viewModelScope.launch {
            val identity = identityRepository.findById(account.identityId)
            _identityLiveData.value = identity
        }
    }

    fun requestGTUDrop() {
        _waitingLiveData.value = true
        proxyRepository.requestGTUDrop(
            account.address,
            {
                _waitingLiveData.value = false
                //populateTransferList()
                createGTUDropTransfer(it.submissionId)
            },
            {
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
                _waitingLiveData.value = false
            }
        )
    }

    private fun updateGTUDropState() {
        if (!BuildConfig.SHOW_GTU_DROP) {
            _showGTUDropLiveData.value = false
        } else {
            _showGTUDropLiveData.value = transferListLiveData.value?.isEmpty()
        }
    }

    private fun createGTUDropTransfer(submissionId: String) {
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        val createdAt = Date().time
        val transfer = Transfer(
            0,
            account.id,
            (-20000000000).toBigInteger(),
            BigInteger.ZERO,
            "",
            account.address,
            expiry,
            "",
            createdAt,
            submissionId,
            TransactionStatus.RECEIVED,
            TransactionOutcome.UNKNOWN,
            TransactionType.TRANSFER,
            null,
            0,
            null
        )
        saveTransfer(transfer)
    }

    private fun saveTransfer(transfer: Transfer) = viewModelScope.launch {
        transferRepository.insert(transfer)
        populateTransferList()
    }

    //region Transaction list update/merge
    // ************************************************************

    private fun clearTransactionListState() {
        _transferListLiveData.value = null
        nonMergedLocalTransactions.clear()
        hasMoreRemoteTransactionsToLoad = true
        lastRemoteTransaction = null
        isLoadingTransactions = false
        allowScrollToLoadMore = true
        lastHeaderDate = null
    }

    fun populateTransferList(notifyWaitingLiveData: Boolean = true) {
        clearTransactionListState()
        if (account.transactionStatus == TransactionStatus.FINALIZED) {
            if (notifyWaitingLiveData) {
                _waitingLiveData.value = true
            }
            viewModelScope.launch {
                updateAccountFromRepository()
                accountUpdater.updateForAccount(account)
                val type =
                    if (account.delegation != null) ProxyRepository.UPDATE_DELEGATION else ProxyRepository.REGISTER_BAKER
                EventBus.getDefault().post(
                    BakerDelegationData(
                        account,
                        isTransactionInProgress = hasPendingDelegationTransactions || hasPendingBakingTransactions,
                        type = type
                    )
                )
            }
        } else {
            _totalBalanceLiveData.value = BigInteger.ZERO
        }
    }

    private fun initializeAccountUpdater() {
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onDone(totalBalances: TotalBalancesData) {
                _totalBalanceLiveData.value = account.balance
                getLocalTransfers()
                viewModelScope.launch {
                    updateAccountFromRepository()
                    _accountUpdatedLiveData.value = true
                }
            }

            override fun onNewAccountFinalized(accountName: String) {
                viewModelScope.launch {
                    _newFinalizedAccountLiveData.value = accountName
                }
            }

            override fun onError(stringRes: Int) {
                _errorLiveData.value = Event(stringRes)
            }
        })
    }

    private suspend fun updateAccountFromRepository() {
        accountRepository.findById(account.id)?.let { accountCandidate ->
            account = accountCandidate
        }
    }

    private fun getIncludeRewards(): String {
        if (session.getHasShowRewards(account.id) && !session.getHasShowFinalizationRewards(account.id)) {
            return "allButFinalization"
        }
        if (session.getHasShowRewards(account.id) && session.getHasShowFinalizationRewards(account.id)) {
            return "all"
        }
        return "none"
    }

    fun getLocalTransfers() {
        viewModelScope.launch {
            hasPendingDelegationTransactions = false
            hasPendingBakingTransactions = false
            val recipientList = recipientRepository.getAll()
            transactionMappingHelper = TransactionMappingHelper(account, recipientList)
            val transferList = transferRepository.getAllByAccountId(account.id)
            for (transfer in transferList) {
                if (transfer.transactionType == TransactionType.LOCAL_DELEGATION)
                    hasPendingDelegationTransactions = true
                if (transfer.transactionType == TransactionType.LOCAL_BAKER)
                    hasPendingBakingTransactions = true
                val transaction = transfer.toTransaction()
                transactionMappingHelper.addTitlesToTransaction(
                    transaction,
                    transfer,
                    getApplication()
                )
                nonMergedLocalTransactions.add(transaction)
            }
            loadRemoteTransactions(null)
        }
    }

    fun loadMoreRemoteTransactions(): Boolean {
        val lastRemote = lastRemoteTransaction
        if (isLoadingTransactions || !hasMoreRemoteTransactionsToLoad || lastRemote == null) {
            return false
        }
        loadRemoteTransactions(lastRemote.id)
        return true
    }

    private fun loadRemoteTransactions(from: Int?) {
        allowScrollToLoadMore = false
        isLoadingTransactions = true
        proxyRepository.getAccountTransactions(
            account.address,
            {
                Log.d("Got more transactions")
                hasMoreRemoteTransactionsToLoad = (it.count >= it.limit)
                if (it.transactions.isNotEmpty()) {
                    lastRemoteTransaction = it.transactions.last()
                }
                mergeTransactions(it.transactions)
                isLoadingTransactions = false
                // Its only for the initial load that it is relevant to disable loading state
                _waitingLiveData.value = false
            },
            {
                Log.d("Get more transactions failed")
                isLoadingTransactions = false
                // Its only for the initial load that it is relevant to disable loading state
                _waitingLiveData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
            }, from = from, limit = 100, includeRewards = getIncludeRewards()
        )
    }

    private fun mergeTransactions(remoteTransactionList: List<RemoteTransaction>) {
        // Convert remote transactions
        val newTransactions: MutableList<Transaction> = ArrayList()
        for (remoteTransaction in remoteTransactionList) {
            val transaction = remoteTransaction.toTransaction()
            transactionMappingHelper.addTitleToTransaction(
                transaction,
                remoteTransaction,
                getApplication()
            )
            newTransactions.add(transaction)
        }
        // Find local transactions to merge
        val localTransactionsToBeMerged: MutableList<Transaction> = ArrayList()
        val localTransactionsToNotMerge: MutableList<Transaction> = ArrayList()
        val lastRemoteTimestamp = getLastRemoteTransactionTimestamp()
        for (ta in nonMergedLocalTransactions) {
            if (ta.timeStamp.time >= lastRemoteTimestamp) {
                localTransactionsToBeMerged.add(ta)
            } else {
                localTransactionsToNotMerge.add(ta)
            }
        }
        nonMergedLocalTransactions = localTransactionsToNotMerge
        // Merge
        newTransactions.addAll(localTransactionsToBeMerged)
        newTransactions.sortByDescending { it.timeStamp }
        // Update list with all transactions to show
        addToTransactionList(newTransactions)
    }

    private fun addToTransactionList(newTransactions: List<Transaction>) {
        val transferList = _transferListLiveData.value ?: ArrayList()
        val adapterList = transferList.toMutableList()
        for (ta in newTransactions) {
            val isAfterHeader = checkToAddHeaderItem(adapterList, ta)
            adapterList.add(
                TransactionItem(
                    transaction = ta,
                    isDividerVisible = !isAfterHeader,
                )
            )
        }
        _transferListLiveData.value = adapterList
        updateGTUDropState()
    }

    private fun checkToAddHeaderItem(
        adapterList: MutableList<AdapterItem>,
        transaction: Transaction
    ): Boolean {
        val lastDate = lastHeaderDate
        val taDate = transaction.timeStamp
        return if (lastDate == null || !DateTimeUtil.isSameDay(lastDate, taDate)) {
            lastHeaderDate = taDate
            adapterList.add(HeaderItem(DateTimeUtil.formatDateAsLocalMediumWithAltTexts(taDate)))
            true
        } else {
            false
        }
    }

    private fun getLastRemoteTransactionTimestamp(): Long {
        val lastRemote = lastRemoteTransaction
        return if (lastRemote == null) {
            0
        } else {
            lastRemote.blockTime.toLong() * 1000
        }
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
                if (first) { //ignore first tick
                    first = false
                    return
                }
                populateTransferList(false)
            }

            override fun onFinish() {
            }
        }
    // endregion
}
