package com.concordium.wallet.ui.connect.uni_ref

import android.app.Application
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
import com.concordium.wallet.data.backend.ws.WsTransport
import com.concordium.wallet.data.cryptolib.ContractAddress
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionInput
import com.concordium.wallet.data.cryptolib.CreateTransferInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountKeyData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.model.TransferSubmissionStatus
import com.concordium.wallet.data.model.WsMessageResponse
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.walletconnect.AccountTransactionPayload
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.connect.TransactionResult
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsViewModel
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PrettyPrint.prettyPrint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.Date

data class WalletData(
    val name: String,
    val address: String,
    val balance: BigInteger
)

class UniRefViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application

    private val proxyRepository = ProxyRepository()

    private val gson = App.appCore.gson

    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val accountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    private val transferRepository =
        TransferRepository(App.appCore.session.walletStorage.database.transferDao())

    private var submitCredentialRequest: BackendRequest<SubmissionData>? = null
    private var transferSubmissionStatusRequest: BackendRequest<TransferSubmissionStatus>? = null
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var accountBalanceRequest: BackendRequest<AccountBalance>? = null
    private var recipientEncryptedKeyRequest: BackendRequest<AccountKeyData>? = null

    lateinit var account: Account
    var isShielded: Boolean = false

    var selectedRecipient: Recipient? = null

    private var tempData = TempData()
    var newTransfer: Transfer? = null

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _gotoSendFundsConfirmLiveData = MutableLiveData<Event<Boolean>>()
    val gotoSendFundsConfirmLiveData: LiveData<Event<Boolean>>
        get() = _gotoSendFundsConfirmLiveData

    private val _backendErrorLiveData = MutableLiveData<Event<BackendError>>()
    val backendErrorLiveData: LiveData<Event<BackendError>>
        get() = _backendErrorLiveData

    private val _transactionFeeLiveData = MutableLiveData<BigInteger>()
    val transactionFeeLiveData: LiveData<BigInteger>
        get() = _transactionFeeLiveData

    private val _transactionResultData = MutableLiveData<TransactionResult>()
    val transactionResultData: LiveData<TransactionResult>
        get() = _transactionResultData

    private val _insufficientFundsLiveData = MutableLiveData<Event<BigInteger>>()
    val insufficientFundsLiveData: LiveData<Event<BigInteger>>
        get() = _insufficientFundsLiveData

    private val _walletDataLiveData = MutableLiveData<Event<WalletData>>()
    val walletDataLiveData: LiveData<Event<WalletData>>
        get() = _walletDataLiveData

    private class TempData {
        var accountNonce: AccountNonce? = null
        var nrgCcdAmount: Long = 0
        var submissionId: String? = null
        var transferSubmissionStatus: TransferSubmissionStatus? = null
        var expiry: Long? = null
        var globalParams: GlobalParams? = null
        var createTransferOutput: CreateTransferOutput? = null
        var newSelfEncryptedAmount: String? = null
        var accountBalance: AccountBalance? = null
        var receiverPublicKey: String? = null
        lateinit var origPayload: WsMessageResponse.Payload
        lateinit var messageType: String
    }

    /**
     * @return false if the account is not found and no further actions can be done.
     */
    private suspend fun loadAccount(): Boolean {
        val address = tempData.origPayload.from
        val accounts = withContext(Dispatchers.IO) {
            accountRepository.getAll()
        }
        val savedAcc = accounts.findLast { it.address == address }
        return if (savedAcc != null) {
            this@UniRefViewModel.account = savedAcc
            _walletDataLiveData.postValue(
                Event(
                    WalletData(
                        savedAcc.getAccountName(),
                        savedAcc.address,
                        savedAcc.balanceAtDisposal
                    )
                )
            )
            true
        } else {
            _errorLiveData.value = Event(12)
            reject()
            false
        }
    }

    private fun checkBalance(min: BigInteger = 5500000.toBigInteger()): Boolean {
        return account.balanceAtDisposal > min
    }

    fun initialize(
        payload: WsMessageResponse.Payload,
        messageType: String
    ) = viewModelScope.launch {
        tempData.messageType = messageType
        tempData.origPayload = payload

        if (!loadAccount()) {
            return@launch
        }
        loadAccountNonce()
        loadTransactionFee()

        if (messageType == WsMessageResponse.MESSAGE_TYPE_SIMPLE_TRANSFER) {
            loadGlobalInfo()
            loadAccountBalance()
            loadRecipientEncryptedKey()
        }
    }

    override fun onCleared() {
        super.onCleared()
        submitCredentialRequest?.dispose()
        transferSubmissionStatusRequest?.dispose()
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("UniRefViewModel#handleBackendError Backend request failed", throwable)
        if (throwable is BackendErrorException) {
            _backendErrorLiveData.value = Event(throwable.error)
        } else {
            _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(throwable))
        }
    }

    private fun loadTransactionFee() {
        val type =
            when (tempData.messageType) {
                WsMessageResponse.MESSAGE_TYPE_SIMPLE_TRANSFER ->
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

                else ->
                    ProxyRepository.UPDATE
            }

        val payload = tempData.origPayload

        proxyRepository.getTransferCost(
            type = type,
            amount = payload.amount,
            sender = payload.from,
            contractIndex = payload.contractAddress?.index?.toIntOrNull(),
            contractSubindex = payload.contractAddress?.subIndex?.toIntOrNull(),
            receiveName = payload.receiveName,
            parameter = payload.serializedParams,
            memoSize = 0,
            success = {
                val nrgCcdAmount =
                    payload.energyLimit * (it.cost.toLong() / it.energy)
                tempData.nrgCcdAmount = nrgCcdAmount
                _transactionFeeLiveData.value = nrgCcdAmount.toBigInteger()
            },
            failure = {
                handleBackendError(it)
            }
        )
    }

    fun isTransferToSameAccount(): Boolean {
        return account.address == selectedRecipient?.address
    }

    fun getAmount(): BigInteger {
        return tempData.origPayload.amount
    }

    fun getTransactionTitle(): String =
        when {
            tempData.messageType == WsMessageResponse.MESSAGE_TYPE_SIMPLE_TRANSFER ->
                context.getString(R.string.transaction_type_transfer)

            tempData.origPayload.getTitle() != null ->
                context.getString(
                    tempData.origPayload.getTitle() ?: R.string.unknown_action
                )

            else ->
                tempData.origPayload.receiveName ?: context.getString(R.string.unknown_action)
        }

    fun getPrettyPrintTransactionDetails(): String =
        tempData.origPayload.toJson().prettyPrint()

    private fun loadAccountNonce() {
        proxyRepository.getAccountNonce(account.address,
            {
                tempData.accountNonce = it
            },
            {
                handleBackendError(it)
            }
        )
    }

    fun sendFunds() {
        val reqAmount = getAmount() + tempData.nrgCcdAmount.toBigInteger()
        if (checkBalance(reqAmount)) {
            _showAuthenticationLiveData.value = Event(true)
        } else {
            _insufficientFundsLiveData.postValue(Event(reqAmount))
        }
    }

    fun reject() {
        // Result without the hash is sent as a rejection.
        WsTransport.sendTransactionResult(
            txHash = null,
            action = null,
        )
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        decryptAndContinue(password)
    }

    private suspend fun decryptAndContinue(password: String) {
        // Decrypt the private data
        val storageAccountDataEncrypted = account.encryptedAccountData
        if (storageAccountDataEncrypted == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return
        }
        val decryptedJson = App.appCore.authManager
            .decrypt(
                password = password,
                encryptedData = storageAccountDataEncrypted,
            )
            ?.let(::String)

        if (decryptedJson != null) {
            val credentialsOutput = gson.fromJson(decryptedJson, StorageAccountData::class.java)
            createTransfer(credentialsOutput.accountKeys, credentialsOutput.encryptionSecretKey)
        } else {
            _errorLiveData.value = Event(R.string.app_error_encryption)
        }
    }

    private suspend fun createTransfer(
        keys: AccountData,
        encryptionSecretKey: String
    ) {
        // Expiry should be now + 10 minutes (in seconds)
        tempData.expiry =
            tempData.origPayload.expiry ?: ((DateTimeUtil.nowPlusMinutes(10).time) / 1000)

        val createTransferOutput: CreateTransferOutput = try {
            when (tempData.messageType) {
                WsMessageResponse.MESSAGE_TYPE_SIMPLE_TRANSFER ->
                    createSimpleTransferTransaction(
                        keys,
                        encryptionSecretKey,
                    )

                else ->
                    createUpdateAccountTransaction(keys)
            }
        } catch (e: Exception) {
            Log.e("Unexpected transfer creation error", e)
            _errorLiveData.value = Event(R.string.app_error_lib)
            return
        }

        tempData.createTransferOutput = createTransferOutput

        submitTransfer(createTransferOutput)
    }

    private suspend fun createUpdateAccountTransaction(
        keys: AccountData,
    ): CreateTransferOutput {
        val createTransactionInput = CreateAccountTransactionInput(
            type = "Update",
            expiry = checkNotNull(tempData.expiry) {
                "The expiry must be set at this moment"
            },
            from = account.address,
            keys = keys,
            nonce = checkNotNull(tempData.accountNonce?.nonce) {
                "The nonce must be loaded at this moment"
            },
            payload = AccountTransactionPayload.Update(
                address = ContractAddress(
                    index = checkNotNull(tempData.origPayload.contractAddress?.index?.toInt()) {
                        "There must be a contract index in the payload"
                    },
                    subIndex = checkNotNull(tempData.origPayload.contractAddress?.subIndex?.toInt()) {
                        "There must be a contract subindex in the payload"
                    },
                ),
                amount = getAmount(),
                maxEnergy = tempData.origPayload.energyLimit,
                maxContractExecutionEnergy = null,
                message = tempData.origPayload.serializedParams ?: "",
                receiveName = tempData.origPayload.receiveName!!,
            )
        )

        val accountTransactionOutput =
            App.appCore.cryptoLibrary.createAccountTransaction(createTransactionInput)
                ?: error("Account transaction creation failed")

        return CreateTransferOutput(
            accountTransactionOutput.signatures,
            accountTransactionOutput.transaction
        )
    }

    private suspend fun createSimpleTransferTransaction(
        keys: AccountData,
        encryptionSecretKey: String,
    ): CreateTransferOutput {
        val transferInput = CreateTransferInput(
            from = account.address,
            keys = keys,
            to = checkNotNull(tempData.origPayload.to) {
                "There must be a recipient address (to) in the payload"
            },
            expiry = checkNotNull(tempData.expiry) {
                "The expiry must be set at this moment"
            },
            amount = getAmount().toString(),
            energy = tempData.origPayload.energyLimit,
            nonce = checkNotNull(tempData.accountNonce?.nonce) {
                "The nonce must be loaded at this moment"
            },
            memo = null,
            global = tempData.globalParams,
            receiverPublicKey = checkNotNull(tempData.receiverPublicKey) {
                "The receiver public key must be loaded at this moment"
            },
            senderSecretKey = encryptionSecretKey,
            inputEncryptedAmount = SendFundsViewModel.calculateInputEncryptedAmount(
                accountId = account.id,
                accountBalance = checkNotNull(tempData.accountBalance) {
                    "The account balance must be loaded at this moment"
                },
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

        return App.appCore.cryptoLibrary.createTransfer(transferInput, transactionType)
            ?: error("Transfer creation failed")
    }

    private fun loadGlobalInfo() {
        // Show waiting state for the full flow, but remove it of any errors occur
        globalParamsRequest?.dispose()
        globalParamsRequest = proxyRepository.getIGlobalInfo(
            {
                tempData.globalParams = it.value
            },
            {
                handleBackendError(it)
            }
        )
    }

    private fun loadAccountBalance() {
        accountBalanceRequest?.dispose()
        accountBalanceRequest = proxyRepository.getAccountBalance(account.address,
            {
                tempData.accountBalance = it
            },
            {
                handleBackendError(it)
            }
        )
    }

    private fun loadRecipientEncryptedKey() {
        recipientEncryptedKeyRequest?.dispose()
        recipientEncryptedKeyRequest = proxyRepository.getAccountEncryptedKey(
            checkNotNull(tempData.origPayload.to) {
                "There must be a recipient address (to) in the payload"
            },
            {
                tempData.receiverPublicKey = it.accountEncryptionKey
            },
            {
                handleBackendError(it)
            }
        )
    }

    private fun submitTransfer(transfer: CreateTransferOutput) {
        submitCredentialRequest?.dispose()
        submitCredentialRequest = proxyRepository.submitTransfer(transfer,
            {
                tempData.submissionId = it.submissionId
                submissionStatus(it.submissionId)
            }, {
                handleBackendError(it)
            }
        )
    }

    private fun submissionStatus(submissionId: String) {
        transferSubmissionStatusRequest?.dispose()
        transferSubmissionStatusRequest =
            proxyRepository.getTransferSubmissionStatus(submissionId,
                {
                    tempData.transferSubmissionStatus = it
                    finishTransferCreation()
                    // Do not disable waiting state yet
                },
                {
                    _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(it))
                }
            )
    }

    private fun finishTransferCreation() {
        val amount = getAmount()
        val submissionId = tempData.submissionId
        val transferSubmissionStatus = tempData.transferSubmissionStatus
        val expiry = tempData.expiry
        val cost = transactionFeeLiveData.value
        val receiveName = tempData.origPayload.receiveName

        WsTransport.sendTransactionResult(
            txHash = tempData.submissionId,
            action = receiveName,
        )

        if (submissionId == null || transferSubmissionStatus == null
            || expiry == null || cost == null
        ) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return
        }
        val createdAt = Date().time
        val newStartIndex: Int = 0
//        tempData.createTransferInput?.let {
//            newStartIndex = it.inputEncryptedAmount?.aggIndex ?: 0
//        }

        val transfer = Transfer(
            0,
            account.id,
            amount,
            cost,
            account.address,
            tempData.origPayload.to ?: "", // Not all transactions are sent to accounts.
            expiry,
            "",
            createdAt,
            submissionId,
            transferSubmissionStatus.status,
            transferSubmissionStatus.outcome ?: TransactionOutcome.UNKNOWN,
            TransactionType.UPDATE,
            tempData.newSelfEncryptedAmount,
            newStartIndex,
            tempData.accountNonce
        )
        newTransfer = transfer
        saveNewTransfer(transfer)

        _transactionResultData.value = TransactionResult(
            amount = amount,
            fee = cost,
            title = getTransactionTitle(),
            submissionId = submissionId,
            transactionStatus = transferSubmissionStatus.status.name
        )
    }

    private fun saveNewTransfer(transfer: Transfer) = viewModelScope.launch {
        _gotoSendFundsConfirmLiveData.value = Event(true)
    }
}
