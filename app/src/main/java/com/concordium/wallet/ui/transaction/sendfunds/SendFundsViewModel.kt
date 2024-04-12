package com.concordium.wallet.ui.transaction.sendfunds

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateTransferInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.InputEncryptedAmount
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.model.TransferSubmissionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.CBORUtil
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.Date

class SendFundsViewModel(application: Application) : AndroidViewModel(application) {

    private val proxyRepository = ProxyRepository()
    private val accountRepository: AccountRepository
    private val transferRepository: TransferRepository

    private val sendFundsPreferences by lazy {
        SendFundsPreferences(getApplication())
    }

    private val gson = App.appCore.gson

    private val accountUpdater = AccountUpdater(application, viewModelScope)

    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var submitCredentialRequest: BackendRequest<SubmissionData>? = null
    private var transferSubmissionStatusRequest: BackendRequest<TransferSubmissionStatus>? = null
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var accountBalanceRequest: BackendRequest<AccountBalance>? = null

    lateinit var account: Account
    var isShielded: Boolean = false

    var selectedRecipient: Recipient? = null
        set(value) {
            field = value
            _recipientLiveData.postValue(value)
            loadTransactionFee()
            // Always load the keys to avoid transfer to
            // not existing account.
            if (value != null && !isTransferToSameAccount()) {
                getAccountEncryptedKey(value.address)
            }
        }
    private var tempData = TempData()
    var newTransfer: Transfer? = null

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _gotoSendFundsConfirmLiveData = MutableLiveData<Event<Boolean>>()
    val gotoSendFundsConfirmLiveData: LiveData<Event<Boolean>>
        get() = _gotoSendFundsConfirmLiveData

    private val _gotoFailedLiveData = MutableLiveData<Event<Pair<Boolean, BackendError?>>>()
    val gotoFailedLiveData: LiveData<Event<Pair<Boolean, BackendError?>>>
        get() = _gotoFailedLiveData

    private val _transactionFeeLiveData = MutableLiveData<BigInteger>()
    val transactionFeeLiveData: LiveData<BigInteger>
        get() = _transactionFeeLiveData

    private val _waitingReceiverAccountPublicKeyLiveData = MutableLiveData<Boolean>()
    val waitingReceiverAccountPublicKeyLiveData: LiveData<Boolean>
        get() = _waitingReceiverAccountPublicKeyLiveData

    private val _recipientLiveData = MutableLiveData<Recipient?>()
    val recipientLiveData: LiveData<Recipient?>
        get() = _recipientLiveData

    private class TempData {
        var accountNonce: AccountNonce? = null
        var toAddress: String? = null
        var amount: BigInteger? = null
        var energy: Long? = null
        var submissionId: String? = null
        var transferSubmissionStatus: TransferSubmissionStatus? = null
        var expiry: Long? = null
        var memo: String? = null
        var globalParams: GlobalParams? = null
        var receiverPublicKey: String? = null
        var accountBalance: AccountBalance? = null
        var createTransferInput: CreateTransferInput? = null
        var createTransferOutput: CreateTransferOutput? = null
        var newSelfEncryptedAmount: String? = null
    }

    init {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        val transferDao = WalletDatabase.getDatabase(application).transferDao()
        transferRepository = TransferRepository(transferDao)
    }

    fun initialize(account: Account, isShielded: Boolean) {
        this.account = account
        this.isShielded = isShielded
        getGlobalInfo()
        getAccountBalance()
    }

    override fun onCleared() {
        super.onCleared()
        accountNonceRequest?.dispose()
        submitCredentialRequest?.dispose()
        transferSubmissionStatusRequest?.dispose()
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        if (throwable is BackendErrorException) {
            _gotoFailedLiveData.postValue(Event(Pair(true, throwable.error)))
        } else {
            _errorLiveData.postValue(Event(BackendErrorHandler.getExceptionStringRes(throwable)))
        }
    }

    private fun loadTransactionFee() {

        val type =
            if (isShielded) {
                if (isTransferToSameAccount()) {
                    ProxyRepository.TRANSFER_TO_PUBLIC
                } else {
                    ProxyRepository.ENCRYPTED_TRANSFER
                }
            } else {
                if (isTransferToSameAccount()) {
                    ProxyRepository.TRANSFER_TO_SECRET
                } else {
                    ProxyRepository.SIMPLE_TRANSFER
                }
            }

        proxyRepository.getTransferCost(
            type = type,
            memoSize = if (tempData.memo == null) 0 else tempData.memo!!.length,
            success = {
                tempData.energy = it.energy
                _transactionFeeLiveData.postValue(it.cost)
            },
            failure = {
                handleBackendError(it)
            }
        )
    }

    fun isTransferToSameAccount(): Boolean {
        return account.address == selectedRecipient?.address
    }

    fun getAmount(): BigInteger? {
        return tempData.amount
    }

    fun hasSufficientFunds(amount: String): Boolean {
        val amountValue = CurrencyUtil.toGTUValue(amount)
        val cost = _transactionFeeLiveData.value
        if (amountValue == null || cost == null) {
            return true
        }

        val totalUnshieldedAtDisposal =
            account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance)

        if (isShielded) {
            if (isTransferToSameAccount()) {
                // SEC_TO_PUBLIC_TRANSFER
                if (amountValue > account.totalShieldedBalance || cost > totalUnshieldedAtDisposal) {
                    return false
                }
            } else {
                // ENCRYPTED_TRANSFER
                if (amountValue > account.totalShieldedBalance || cost > totalUnshieldedAtDisposal) {
                    return false
                }
            }
        } else {
            if (isTransferToSameAccount()) {
                // PUBLIC_TO_SEC_TRANSFER
                if (amountValue + cost > totalUnshieldedAtDisposal) {
                    return false
                }
            } else {
                // REGULAR_TRANSFER
                if (amountValue + cost > totalUnshieldedAtDisposal) {
                    return false
                }
            }
        }
        return true
    }

    fun sendFunds(amount: String) {
        val amountValue = CurrencyUtil.toGTUValue(amount)
        if (amountValue == null) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            return
        }
        tempData.amount = amountValue

        val recipient = selectedRecipient
        if (recipient == null) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            return
        }
        tempData.toAddress = recipient.address
        getAccountNonce()
    }

    private fun getAccountNonce() {
        _waitingLiveData.postValue(true)
        accountNonceRequest?.dispose()
        accountNonceRequest = proxyRepository.getAccountNonce(account.address,
            {
                tempData.accountNonce = it
                _showAuthenticationLiveData.postValue(Event(true))
                _waitingLiveData.postValue(false)
            },
            {
                _waitingLiveData.postValue(false)
                handleBackendError(it)
            }
        )
    }

    fun continueWithPassword(password: String) = viewModelScope.launch(Dispatchers.IO) {
        _waitingLiveData.postValue(true)
        decryptAndContinue(password)
    }

    private suspend fun decryptAndContinue(password: String) {
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
            val credentialsOutput = gson.fromJson(decryptedJson, StorageAccountData::class.java)
            createTransfer(credentialsOutput.accountKeys, credentialsOutput.encryptionSecretKey)
        } else {
            _errorLiveData.postValue(Event(R.string.app_error_encryption))
            _waitingLiveData.postValue(false)
        }
    }

    private suspend fun createTransfer(
        keys: AccountData,
        encryptionSecretKey: String
    ) {
        val toAddress = tempData.toAddress
        val nonce = tempData.accountNonce
        val amount = tempData.amount
        val energy = tempData.energy
        val memo = tempData.memo
        val balance = tempData.accountBalance

        if (toAddress == null || nonce == null || amount == null || energy == null
            || balance == null
        ) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            _waitingLiveData.postValue(false)
            return
        }

        // Expiry should me now + 10 minutes (in seconds)
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        tempData.expiry = expiry
        val transferInput = CreateTransferInput(
            account.address,
            keys,
            toAddress,
            expiry,
            amount.toString(),
            energy,
            nonce.nonce,
            memo,
            tempData.globalParams,
            tempData.receiverPublicKey,
            encryptionSecretKey,
            calculateInputEncryptedAmount(
                accountId = account.id,
                accountBalance = balance,
                transferRepository = transferRepository,
                accountUpdater = accountUpdater,
            ),
            null,
            null,
            null,
        )

        val transactionType =
            if (isShielded) {
                if (isTransferToSameAccount()) {
                    CryptoLibrary.SEC_TO_PUBLIC_TRANSFER
                } else {
                    CryptoLibrary.ENCRYPTED_TRANSFER
                }
            } else {
                if (isTransferToSameAccount()) {
                    CryptoLibrary.PUBLIC_TO_SEC_TRANSFER
                } else {
                    CryptoLibrary.REGULAR_TRANSFER
                }
            }
        tempData.createTransferInput = transferInput

        val output = App.appCore.cryptoLibrary.createTransfer(transferInput, transactionType)
        if (output == null) {
            _errorLiveData.postValue(Event(R.string.app_error_lib))
            _waitingLiveData.postValue(false)
        } else {
            tempData.createTransferOutput = output

            // Save and update new selfAmount for later lookup
            viewModelScope.launch {
                if (output.addedSelfEncryptedAmount != null) {
                    account.finalizedEncryptedBalance?.let { encBalance ->
                        val newEncryptedAmount = App.appCore.cryptoLibrary.combineEncryptedAmounts(
                            output.addedSelfEncryptedAmount,
                            encBalance.selfAmount
                        ).toString()
                        tempData.newSelfEncryptedAmount = newEncryptedAmount
                        val oldDecryptedAmount =
                            accountUpdater.lookupMappedAmount(encBalance.selfAmount)
                        oldDecryptedAmount?.let {
                            accountUpdater.saveDecryptedAmount(
                                newEncryptedAmount,
                                (it.toBigInteger() + amount).toString()
                            )
                        }
                    }
                }
                if (output.remaining != null) {
                    tempData.newSelfEncryptedAmount = output.remaining
                    val remainingAmount =
                        accountUpdater.decryptAndSaveAmount(encryptionSecretKey, output.remaining)

                    account.finalizedEncryptedBalance?.let { encBalance ->
                        val oldDecryptedAmount =
                            accountUpdater.lookupMappedAmount(encBalance.selfAmount)
                        oldDecryptedAmount?.let {
                            accountUpdater.saveDecryptedAmount(
                                output.remaining,
                                remainingAmount.toString()
                            )
                        }
                    }
                }
                submitTransfer(output)
            }
        }
    }

    private fun getGlobalInfo() {
        // Show waiting state for the full flow, but remove it of any errors occur
        _waitingLiveData.postValue(true)
        globalParamsRequest?.dispose()
        globalParamsRequest = proxyRepository.getIGlobalInfo(
            {
                tempData.globalParams = it.value
                _waitingLiveData.postValue(false)
            },
            {
                _waitingLiveData.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun getAccountEncryptedKey(accountAddress: String) {
        _waitingReceiverAccountPublicKeyLiveData.postValue(true)
        proxyRepository.getAccountEncryptedKey(
            accountAddress,
            {
                tempData.receiverPublicKey = it.accountEncryptionKey
                _waitingReceiverAccountPublicKeyLiveData.postValue(false)
            },
            {
                _waitingReceiverAccountPublicKeyLiveData.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun getAccountBalance() {
        _waitingLiveData.postValue(true)
        accountBalanceRequest?.dispose()
        accountBalanceRequest = proxyRepository.getAccountBalance(account.address,
            {
                tempData.accountBalance = it
                _waitingLiveData.postValue(false)
            },
            {
                _waitingLiveData.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun submitTransfer(transfer: CreateTransferOutput) {
        _waitingLiveData.postValue(true)
        submitCredentialRequest?.dispose()
        submitCredentialRequest = proxyRepository.submitTransfer(transfer,
            {
                tempData.submissionId = it.submissionId
                submissionStatus(it.submissionId)
                // Do not disable waiting state yet
            },
            {
                _waitingLiveData.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun submissionStatus(submissionId: String) {
        _waitingLiveData.postValue(true)
        transferSubmissionStatusRequest?.dispose()
        transferSubmissionStatusRequest = proxyRepository.getTransferSubmissionStatus(submissionId,
            {
                tempData.transferSubmissionStatus = it
                accountUpdater.updateEncryptedAmount(it, submissionId, tempData.amount?.toString())
                finishTransferCreation()
                // Do not disable waiting state yet
            },
            {
                _waitingLiveData.postValue(false)
                _errorLiveData.postValue(Event(BackendErrorHandler.getExceptionStringRes(it)))
            }
        )
    }

    private fun finishTransferCreation() {
        val amount = tempData.amount
        val toAddress = tempData.toAddress
        val memo = tempData.memo
        val submissionId = tempData.submissionId
        val transferSubmissionStatus = tempData.transferSubmissionStatus
        val expiry = tempData.expiry
        val cost = transactionFeeLiveData.value

        if (amount == null || toAddress == null || submissionId == null ||
            transferSubmissionStatus == null || expiry == null || cost == null
        ) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            _waitingLiveData.postValue(false)
            return
        }
        val createdAt = Date().time
        var newStartIndex: Int = 0
        tempData.createTransferInput?.let {
            newStartIndex = it.inputEncryptedAmount?.aggIndex ?: 0
        }

        val transfer = Transfer(
            0,
            account.id,
            amount,
            cost,
            account.address,
            toAddress,
            expiry,
            memo,
            createdAt,
            submissionId,
            transferSubmissionStatus.status,
            transferSubmissionStatus.outcome ?: TransactionOutcome.UNKNOWN,
            if (isShielded) {
                if (isTransferToSameAccount()) {
                    if (memo == null || memo.isEmpty()) {
                        TransactionType.TRANSFER
                    } else {
                        TransactionType.TRANSFERWITHMEMO
                    }
                } else {
                    if (memo == null || memo.isEmpty()) {
                        TransactionType.ENCRYPTEDAMOUNTTRANSFER
                    } else {
                        TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO
                    }
                }
            } else {
                if (isTransferToSameAccount()) {
                    TransactionType.TRANSFERTOENCRYPTED
                } else {
                    TransactionType.TRANSFERTOPUBLIC
                }
            },
            tempData.newSelfEncryptedAmount,
            newStartIndex,
            tempData.accountNonce
        )
        newTransfer = transfer
        saveNewTransfer(transfer)
    }

    private fun saveNewTransfer(transfer: Transfer) = viewModelScope.launch(Dispatchers.IO) {
        transferRepository.insert(transfer)

        _gotoSendFundsConfirmLiveData.postValue(Event(true))
    }

    fun ByteArray.toHexString() = joinToString("") {
        Integer.toUnsignedString(java.lang.Byte.toUnsignedInt(it), 16).padStart(2, '0')
    }

    fun setMemo(memo: ByteArray?) {
        if (memo != null) {
            tempData.memo = memo.toHexString()
        } else {
            tempData.memo = null
        }
        loadTransactionFee()
    }

    fun showMemoWarning(): Boolean {
        return sendFundsPreferences.showMemoWarning()
    }

    fun dontShowMemoWarning() {
        return sendFundsPreferences.dontShowMemoWarning()
    }

    fun getClearTextMemo(): String? {
        if (tempData.memo == null) {
            return null
        } else {
            return CBORUtil.decodeHexAndCBOR(tempData.memo!!)
        }
    }

    companion object {
        suspend fun calculateInputEncryptedAmount(
            accountId: Int,
            accountBalance: AccountBalance,
            transferRepository: TransferRepository,
            accountUpdater: AccountUpdater,
        ): InputEncryptedAmount {
            val lastNounceToInclude = accountBalance.finalizedBalance?.accountNonce ?: -2

            val allTransfers = transferRepository.getAllByAccountId(accountId)
            val unfinalisedTransfers = allTransfers.filter {
                it.transactionStatus != TransactionStatus.FINALIZED && (it.nonce?.nonce
                    ?: -1) >= lastNounceToInclude
            }

            val aggEncryptedAmount = if (unfinalisedTransfers.isNotEmpty()) {
                val lastTransaction =
                    unfinalisedTransfers.maxWithOrNull(Comparator { a, b -> a.id.compareTo(b.id) })
                if (lastTransaction != null) {
                    accountBalance.finalizedBalance?.let {
                        val incomingAmounts = it.accountEncryptedAmount.incomingAmounts.filter {
                            accountUpdater.lookupMappedAmount(it) != null
                        }
                        var agg = lastTransaction.newSelfEncryptedAmount ?: ""
                        for (i in lastTransaction.newStartIndex until it.accountEncryptedAmount.startIndex + incomingAmounts.count()) {
                            agg = App.appCore.cryptoLibrary.combineEncryptedAmounts(
                                agg,
                                incomingAmounts[i]
                            ).toString()
                        }
                        agg
                    } ?: ""
                } else {
                    ""
                }
            } else {
                accountBalance.finalizedBalance?.let {
                    var agg = it.accountEncryptedAmount.selfAmount
                    it.accountEncryptedAmount.incomingAmounts.forEach {
                        if (accountUpdater.lookupMappedAmount(it) != null) {
                            agg = App.appCore.cryptoLibrary.combineEncryptedAmounts(agg, it)
                                .toString()
                        }
                    }
                    agg
                } ?: ""
            }

            val aggAmount = accountBalance.finalizedBalance?.let {
                var agg =
                    accountUpdater.lookupMappedAmount(it.accountEncryptedAmount.selfAmount)
                        ?.toBigInteger()
                        ?: BigInteger.ZERO
                it.accountEncryptedAmount.incomingAmounts.forEach {
                    agg += accountUpdater.lookupMappedAmount(it)?.toBigInteger() ?: BigInteger.ZERO
                }
                unfinalisedTransfers.forEach {
                    agg -= it.amount
                }
                agg
            } ?: ""

            val index = accountBalance.finalizedBalance?.let {
                it.accountEncryptedAmount.startIndex + it.accountEncryptedAmount.incomingAmounts.count {
                    accountUpdater.lookupMappedAmount(it) != null
                }
            } ?: 0

            return InputEncryptedAmount(aggEncryptedAmount, aggAmount.toString(), index)
        }
    }
}
