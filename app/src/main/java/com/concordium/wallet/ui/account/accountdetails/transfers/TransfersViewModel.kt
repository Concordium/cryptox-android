package com.concordium.wallet.ui.account.accountdetails.transfers

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.Session
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.RemoteTransaction
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.util.toTransaction
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.TransactionMappingHelper
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.account.common.accountupdater.TotalBalancesData
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.Date

class TransfersViewModel(
    application: Application,
    mainViewModel: MainViewModel,
) : AndroidViewModel(application) {

    private val session: Session = App.appCore.session
    private val proxyRepository = ProxyRepository()
    private val transferRepository =
        TransferRepository(session.walletStorage.database.transferDao())

    private val transactionMappingHelper = TransactionMappingHelper()
    private val accountUpdater = AccountUpdater(application, viewModelScope)

    private var isLoadingTransactions = false
    private var lastRemoteTransaction: RemoteTransaction? = null
    private var nonMergedLocalTransactions: MutableList<Transaction> = ArrayList()
    private var lastHeaderDate: Date? = null
    var allowScrollToLoadMore = true
    var hasMoreRemoteTransactionsToLoad = true
        private set

    private lateinit var account: Account

    private val _transferListFlow = MutableStateFlow<List<AdapterItem>?>(emptyList())
    val transferListFlow = _transferListFlow.asStateFlow()

    private val _showGTUDropFlow = MutableStateFlow(false)
    val showGTUDropFlow = _showGTUDropFlow.asStateFlow()

    private val _totalBalanceFlow = MutableStateFlow<BigInteger>(BigInteger.ZERO)
    val totalBalanceFlow = _totalBalanceFlow.asStateFlow()

    private val _waitingFlow = MutableStateFlow(false)
    val waitingFlow = _waitingFlow.asStateFlow()

    private val _errorFlow = MutableStateFlow(Event(-1))
    val errorFlow = _errorFlow.asStateFlow()

    init {
        initializeAccountUpdater()

        viewModelScope.launch {
            mainViewModel.activeAccount.collect { account ->
                account?.let(::updateAccount)
            }
        }
    }

    fun updateAccount(account: Account) {
        this.account = account
        _totalBalanceFlow.value = account.balance

        clearTransactionListState()
        _waitingFlow.value = true
        getLocalTransfers()
        _waitingFlow.value = false
    }

    fun getAccount(): Account = account

    private fun initializeAccountUpdater() {
        accountUpdater.setUpdateListener(object : AccountUpdater.UpdateListener {
            override fun onDone(totalBalances: TotalBalancesData) {
                _totalBalanceFlow.value = account.balance
            }

            override fun onNewAccountFinalized(accountName: String) {

            }

            override fun onError(stringRes: Int) {
                _errorFlow.value = Event(stringRes)
            }
        })
    }

    private fun getLocalTransfers() = viewModelScope.launch {
        val transferList = transferRepository.getAllByAccountId(account.id)
        for (transfer in transferList) {
            val transaction = transfer.toTransaction()
            transactionMappingHelper.addTitlesToTransaction(
                transaction,
                getApplication()
            )
            nonMergedLocalTransactions.add(transaction)
        }
        loadRemoteTransactions(null)
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
                _waitingFlow.value = false
            },
            {
                Log.d("Get more transactions failed")
                isLoadingTransactions = false
                // Its only for the initial load that it is relevant to disable loading state
                _waitingFlow.value = false
                _errorFlow.value = Event(BackendErrorHandler.getExceptionStringRes(it))
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

    private fun getLastRemoteTransactionTimestamp(): Long {
        val lastRemote = lastRemoteTransaction
        return if (lastRemote == null) {
            0
        } else {
            lastRemote.blockTime.toLong() * 1000
        }
    }

    private fun addToTransactionList(newTransactions: List<Transaction>) {
        val adapterList = (_transferListFlow.value ?: emptyList()).toMutableList()
        for (ta in newTransactions) {
            val isAfterHeader = checkToAddHeaderItem(adapterList, ta)
            adapterList.add(
                TransactionItem(
                    transaction = ta,
                    isDividerVisible = !isAfterHeader,
                )
            )
        }
        _transferListFlow.value = adapterList
        updateGTUDropState()
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

    private fun checkToAddHeaderItem(
        adapterList: MutableList<AdapterItem>,
        transaction: Transaction,
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

    fun populateTransferList(notifyWaitingLiveData: Boolean = true) {
        clearTransactionListState()
        if (account.transactionStatus == TransactionStatus.FINALIZED) {
            if (notifyWaitingLiveData) {
                _waitingFlow.value = true
            }
            viewModelScope.launch {
                accountUpdater.updateForAccount(account)
            }
        } else {
            _totalBalanceFlow.value = BigInteger.ZERO
        }
    }

    fun requestGTUDrop() {
        _waitingFlow.value = true
        proxyRepository.requestGTUDrop(
            account.address,
            {
                _waitingFlow.value = false
                createGTUDropTransfer(it.submissionId)
            },
            {
                _errorFlow.value = Event(BackendErrorHandler.getExceptionStringRes(it))
                _waitingFlow.value = false
            }
        )
    }

    private fun createGTUDropTransfer(submissionId: String) {
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        val createdAt = Date().time
        val transfer = Transfer(
            id = 0,
            accountId = account.id,
            amount = (-20000000000).toBigInteger(),
            cost = BigInteger.ZERO,
            fromAddress = "",
            toAddress = account.address,
            expiry = expiry,
            memo = null,
            createdAt = createdAt,
            submissionId = submissionId,
            transactionStatus = TransactionStatus.RECEIVED,
            outcome = TransactionOutcome.UNKNOWN,
            transactionType = TransactionType.TRANSFER,
            tokenTransferAmount = null,
            tokenSymbol = null,
        )
        saveTransfer(transfer)
    }

    private fun updateGTUDropState() {
        if (!BuildConfig.SHOW_GTU_DROP) {
            _showGTUDropFlow.value = false
        } else {
            _showGTUDropFlow.value = transferListFlow.value.isNullOrEmpty()

        }
    }

    private fun saveTransfer(transfer: Transfer) = viewModelScope.launch {
        transferRepository.insert(transfer)
        populateTransferList()
    }

    private fun clearTransactionListState() {
        _transferListFlow.value = emptyList()
        nonMergedLocalTransactions.clear()
        hasMoreRemoteTransactionsToLoad = true
        lastRemoteTransaction = null
        isLoadingTransactions = false
        allowScrollToLoadMore = true
        lastHeaderDate = null
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
}
