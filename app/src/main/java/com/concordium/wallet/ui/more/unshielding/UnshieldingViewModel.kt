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
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateTransferInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.TransactionCost
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsViewModel
import com.concordium.wallet.util.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UnshieldingViewModel(application: Application) : AndroidViewModel(application) {
    private val accountRepository: AccountRepository by lazy {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        AccountRepository(accountDao)
    }
    private val proxyRepository: ProxyRepository by lazy(::ProxyRepository)
    private val transferRepository: TransferRepository by lazy {
        val transferDao = WalletDatabase.getDatabase(application).transferDao()
        TransferRepository(transferDao)
    }
    private val accountUpdater: AccountUpdater by lazy {
        AccountUpdater(application, viewModelScope)
    }
    private var isInitialized = false
    private lateinit var account: Account
    private lateinit var accountNonce: AccountNonce
    private lateinit var accountBalance: AccountBalance
    private lateinit var transactionCost: TransactionCost
    private lateinit var globalParams: GlobalParams

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean> = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>> = _errorLiveData

    private val _titleLiveData = MutableLiveData<String>()
    val titleLiveData: LiveData<String> = _titleLiveData

    private val _amountLiveData = MutableLiveData<BigInteger>()
    val amountLiveData: LiveData<BigInteger> = _amountLiveData

    private val _transactionCostLiveData = MutableLiveData<BigInteger>()
    val transactionCostLiveData: LiveData<BigInteger> = _transactionCostLiveData

    private val _insufficientFundsLiveData = MutableLiveData<Boolean>()
    val insufficientFundsLiveData: LiveData<Boolean> = _insufficientFundsLiveData

    private val _isUnshieldEnabledLiveData = MutableLiveData<Boolean>()
    val isUnshieldEnabledLiveData: LiveData<Boolean> = _isUnshieldEnabledLiveData

    private val _showAuthLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthLiveData: LiveData<Event<Boolean>> = _showAuthLiveData

    private val _finishWithResultLiveData = MutableLiveData<Event<UnshieldingResult>>()
    val finishWithResultLiveData: LiveData<Event<UnshieldingResult>> = _finishWithResultLiveData

    fun initializeOnce(accountAddress: String) = viewModelScope.launch {
        if (isInitialized) {
            return@launch
        }

        account = accountRepository.findByAddress(accountAddress)
            ?: error("Requested account $accountAddress not found")
        _amountLiveData.postValue(account.totalShieldedBalance)
        _titleLiveData.postValue(account.getAccountName())

        isInitialized = true

        prepareForUnshielding()
    }

    private suspend fun prepareForUnshielding() {
        _waitingLiveData.postValue(true)
        _isUnshieldEnabledLiveData.postValue(false)

        try {
            accountNonce = getAccountNonce()
            accountBalance = proxyRepository.getAccountBalanceSuspended(account.address)
            transactionCost = getTransferCost()
            globalParams = getGlobalParams()
        } catch (e: Exception) {
            _errorLiveData.postValue(Event(BackendErrorHandler.getExceptionStringRes(e)))
            _waitingLiveData.postValue(false)
            return
        }

        _transactionCostLiveData.value = transactionCost.cost
        val isEnoughFunds = transactionCost.cost <= getBalanceAtDisposal()
        _isUnshieldEnabledLiveData.postValue(isEnoughFunds)
        _insufficientFundsLiveData.postValue(!isEnoughFunds)

        _waitingLiveData.postValue(false)
    }

    private suspend fun getAccountNonce(
    ): AccountNonce = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getAccountNonce(
            accountAddress = account.address,
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private suspend fun getTransferCost(
    ): TransactionCost = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getTransferCost(
            type = ProxyRepository.TRANSFER_TO_PUBLIC,
            memoSize = 0,
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private suspend fun getGlobalParams(
    ): GlobalParams = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getIGlobalInfo(
            success = { continuation.resume(it.value) },
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    fun onUnshieldClicked() {
        check(_isUnshieldEnabledLiveData.value == true) {
            "Unshield can only be clicked if it is enabled"
        }

        _showAuthLiveData.postValue(Event(true))
    }

    fun onAuthenticated(password: String) = viewModelScope.launch(Dispatchers.IO) {
        decryptAndUnshield(password)
    }

    private suspend fun decryptAndUnshield(password: String) {
        _waitingLiveData.postValue(true)
        _isUnshieldEnabledLiveData.postValue(false)

        // This method is mostly a copypaste from former SendFundsViewModel.

        val storageAccountDataEncrypted = account.encryptedAccountData
        if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            _waitingLiveData.postValue(false)
            _isUnshieldEnabledLiveData.postValue(true)
            return
        }
        val decryptedJson = App.appCore.getCurrentAuthenticationManager()
            .decryptInBackground(password, storageAccountDataEncrypted)

        if (decryptedJson == null) {
            _errorLiveData.postValue(Event(R.string.app_error_encryption))
            _waitingLiveData.postValue(false)
            _isUnshieldEnabledLiveData.postValue(true)
            return
        }

        val credentialsOutput =
            App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
        // Expiry should me now + 10 minutes (in seconds)
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        val amount = account.totalShieldedBalance
        val createTransferInput = CreateTransferInput(
            from = account.address,
            keys = credentialsOutput.accountKeys,
            to = account.address,
            expiry = expiry,
            amount = amount.toString(),
            energy = transactionCost.energy,
            nonce = accountNonce.nonce,
            memo = null,
            global = globalParams,
            receiverPublicKey = credentialsOutput.encryptionSecretKey,
            senderSecretKey = credentialsOutput.encryptionSecretKey,
            inputEncryptedAmount = SendFundsViewModel.calculateInputEncryptedAmount(
                accountId = account.id,
                accountBalance = accountBalance,
                transferRepository = transferRepository,
                accountUpdater = accountUpdater,
            ),
            capital = null,
            restakeEarnings = null,
            delegationTarget = null,
        )
        val createTransferOutput = App.appCore.cryptoLibrary.createTransfer(
            createTransferInput = createTransferInput,
            type = CryptoLibrary.SEC_TO_PUBLIC_TRANSFER
        )

        if (createTransferOutput == null) {
            _errorLiveData.postValue(Event(R.string.app_error_lib))
            _waitingLiveData.postValue(false)
            _isUnshieldEnabledLiveData.postValue(true)
            return
        }

        val submissionId: String
        try {
            submissionId = submitTransfer(createTransferOutput)
        } catch (e: Exception) {
            _errorLiveData.postValue(Event(BackendErrorHandler.getExceptionStringRes(e)))
            _waitingLiveData.postValue(false)
            _isUnshieldEnabledLiveData.postValue(true)
            return
        }

        onUnshielded(
            createTransferInput = createTransferInput,
            createTransferOutput = createTransferOutput,
            credentialsOutput = credentialsOutput,
            amount = amount,
            submissionId = submissionId
        )

        _isUnshieldEnabledLiveData.postValue(true)
        _waitingLiveData.postValue(false)
    }

    private suspend fun submitTransfer(
        createTransferOutput: CreateTransferOutput,
    ): String = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.submitTransfer(
            transfer = createTransferOutput,
            success = { continuation.resume(it.submissionId) },
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private suspend fun onUnshielded(
        createTransferInput: CreateTransferInput,
        createTransferOutput: CreateTransferOutput,
        credentialsOutput: StorageAccountData,
        amount: BigInteger,
        submissionId: String,
    ) {
        var newSelfEncryptedAmount: String? = null

        if (createTransferOutput.remaining != null) {
            newSelfEncryptedAmount = createTransferOutput.remaining
            val remainingAmount =
                accountUpdater.decryptAndSaveAmount(
                    credentialsOutput.encryptionSecretKey,
                    createTransferOutput.remaining
                )

            account.finalizedEncryptedBalance?.let { encBalance ->
                val oldDecryptedAmount =
                    accountUpdater.lookupMappedAmount(encBalance.selfAmount)
                oldDecryptedAmount?.let {
                    accountUpdater.saveDecryptedAmount(
                        createTransferOutput.remaining,
                        remainingAmount.toString()
                    )
                }
            }
        }

        val newTransfer = Transfer(
            id = 0,
            accountId = account.id,
            amount = amount,
            cost = transactionCost.cost,
            fromAddress = account.address,
            toAddress = account.address,
            expiry = createTransferInput.expiry,
            memo = null,
            createdAt = System.currentTimeMillis(),
            submissionId = submissionId,
            transactionStatus = TransactionStatus.RECEIVED,
            outcome = TransactionOutcome.UNKNOWN,
            transactionType = TransactionType.TRANSFER,
            newSelfEncryptedAmount = newSelfEncryptedAmount,
            newStartIndex = createTransferInput.inputEncryptedAmount?.aggIndex ?: 0,
            nonce = accountNonce
        )
        transferRepository.insert(newTransfer)

        _finishWithResultLiveData.postValue(
            Event(
                UnshieldingResult(
                    unshieldedAmount = amount,
                    accountAddress = account.address,
                )
            )
        )
    }

    private fun getBalanceAtDisposal(): BigInteger =
        account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance)

    override fun onCleared() {
        super.onCleared()
        accountUpdater.dispose()
    }
}
