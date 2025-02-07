package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.ContractAddress
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionInput
import com.concordium.wallet.data.cryptolib.CreateTransferInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersInput
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersOutput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.preferences.WalletSendFundsPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.util.toTransaction
import com.concordium.wallet.data.walletconnect.AccountTransactionPayload
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.transaction.sendfunds.SendFundsViewModel
import com.concordium.wallet.util.CBORUtil
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toHex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable
import java.math.BigInteger
import java.util.Date

data class SendTokenData(
    var token: Token? = null,
    var account: Account? = null,
    var amount: BigInteger = BigInteger.ZERO,
    /**
     * A valid address or an empty string.
     */
    var receiver: String = "",
    var receiverName: String? = null,
    var fee: BigInteger? = null,
    var max: BigInteger? = null,
    var memo: String? = null,
    var energy: Long? = null,
    var accountNonce: AccountNonce? = null,
    var expiry: Long? = null,
    var createTransferInput: CreateTransferInput? = null,
    var createTransferOutput: CreateTransferOutput? = null,
    var receiverPublicKey: String? = null,
    var globalParams: GlobalParams? = null,
    var accountBalance: AccountBalance? = null,
    var newSelfEncryptedAmount: String? = null
) : Serializable

class SendTokenViewModel(application: Application) : AndroidViewModel(application), Serializable {
    companion object {
        const val SEND_TOKEN_DATA = "SEND_TOKEN_DATA"
    }

    private val proxyRepository = ProxyRepository()
    private val transferRepository =
        TransferRepository(App.appCore.session.walletStorage.database.transferDao())
    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val sendFundsPreferences: WalletSendFundsPreferences =
        App.appCore.session.walletStorage.sendFundsPreferences

    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var submitTransaction: BackendRequest<SubmissionData>? = null
    private var accountBalanceRequest: BackendRequest<AccountBalance>? = null

    var sendTokenData = SendTokenData()
    val tokens: MutableLiveData<List<Token>> by lazy { MutableLiveData<List<Token>>() }
    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    val transactionReady: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val feeReady: MutableLiveData<BigInteger?> by lazy { MutableLiveData<BigInteger?>(null) }
    val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val showAuthentication: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transactionWaiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transaction: MutableLiveData<Transaction> by lazy { MutableLiveData<Transaction>() }
    val eurRateReady: MutableLiveData<BigInteger?> by lazy { MutableLiveData<BigInteger?>() }

    val canSend: Boolean
        get() = with(sendTokenData) {
            token != null
                    && receiver.isNotBlank()
                    && amount.signum() > 0
                    && fee != null
                    && hasEnoughFunds()
        }

    init {
        chooseToken.observeForever { token ->
            sendTokenData.token = token
            sendTokenData.max = if (token.isCcd) null else token.balance
            sendTokenData.fee = null
            sendTokenData.amount = BigInteger.ZERO
            feeReady.value = null
        }
    }

    fun dispose() {
        accountNonceRequest?.dispose()
        globalParamsRequest?.dispose()
        submitTransaction?.dispose()
        accountBalanceRequest?.dispose()
    }

    fun loadTokens(accountAddress: String) {
        waiting.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val contractTokensRepository = ContractTokensRepository(
                App.appCore.session.walletStorage.database.contractTokenDao()
            )
            val tokensFound = mutableListOf<Token>()
            tokensFound.add(getCCDDefaultToken(accountAddress))
            val contractTokens = contractTokensRepository.getTokens(
                accountAddress = accountAddress,
            )
            tokensFound.addAll(contractTokens.map(::Token))
            waiting.postValue(false)
            tokens.postValue(tokensFound)
        }
    }

    fun getGlobalInfo() {
        waiting.postValue(true)
        globalParamsRequest?.dispose()
        globalParamsRequest = proxyRepository.getIGlobalInfo(
            success = {
                sendTokenData.globalParams = it.value
                waiting.postValue(false)
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    fun getAccountBalance() {
        if (sendTokenData.account == null)
            return

        waiting.postValue(true)
        accountBalanceRequest?.dispose()
        accountBalanceRequest = proxyRepository.getAccountBalance(
            accountAddress = sendTokenData.account!!.address,
            success = {
                sendTokenData.accountBalance = it
                waiting.postValue(false)
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    fun send() {
        waiting.postValue(true)
        accountNonceRequest?.dispose()
        accountNonceRequest = proxyRepository.getAccountNonce(
            accountAddress = sendTokenData.account?.address ?: "",
            success = {
                waiting.postValue(false)
                sendTokenData.accountNonce = it
                showAuthentication.postValue(true)
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            })
    }

    fun setMemo(memo: ByteArray?) {
        sendTokenData.memo = memo?.toHex()
        loadTransactionFee()
    }

    fun getMemoText(): String? =
        sendTokenData.memo?.let(CBORUtil.Companion::decodeHexAndCBOR)

    fun showMemoWarning(): Boolean {
        return sendFundsPreferences.shouldShowMemoWarning()
    }

    fun dontShowMemoWarning() {
        return sendFundsPreferences.disableShowMemoWarning()
    }

    fun onReceiverEntered(input: String) {
        sendTokenData.receiverName = null

        if (App.appCore.cryptoLibrary.checkAccountAddress(input)) {
            sendTokenData.receiver = input

            // Fee should be updated only when sending CIS-2.
            if (sendTokenData.token?.isCcd == false) {
                loadTransactionFee()
            }
        } else {
            sendTokenData.receiver = ""
        }
    }

    fun onReceiverNameFound(name: String) {
        sendTokenData.receiverName = name
    }

    fun loadEURRate() {
        if (sendTokenData.token == null)
            return

        if (sendTokenData.token!!.isCcd) {
            viewModelScope.launch {
                getTransferEURRate()
            }
        }
    }

    fun loadTransactionFee() {
        if (sendTokenData.token == null)
            return

        if (sendTokenData.token!!.isCcd) {
            waiting.postValue(true)
            viewModelScope.launch {
                getTransferCostCCD()
            }
            return
        }

        if (sendTokenData.account == null || sendTokenData.receiver.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(
                sendTokenData.token!!.token,
                sendTokenData.amount.toString(),
                sendTokenData.account!!.address,
                sendTokenData.receiver
            )
            val serializeTokenTransferParametersOutput =
                App.appCore.cryptoLibrary.serializeTokenTransferParameters(
                    serializeTokenTransferParametersInput
                )
            if (serializeTokenTransferParametersOutput == null) {
                waiting.postValue(false)
                errorInt.postValue(R.string.app_error_lib)
            } else {
                getTransferCost(serializeTokenTransferParametersOutput)
            }
        }
    }

    fun hasEnoughFunds(): Boolean {
        if (sendTokenData.token == null)
            return false

        var atDisposal: BigInteger = BigInteger.ZERO
        sendTokenData.account?.let { account ->
            atDisposal = account.balanceAtDisposal
        }

        return if (sendTokenData.token!!.isCcd) {
            atDisposal >= sendTokenData.amount + (sendTokenData.fee ?: BigInteger.ZERO)
        } else {
            atDisposal >= (sendTokenData.fee
                ?: BigInteger.ZERO) && sendTokenData.token!!.balance >= sendTokenData.amount
        }
    }

    private fun getTransferEURRate() {
        proxyRepository.getChainParameters(
            success = { response ->
                Log.d("sendTokenData.amount: ${sendTokenData.amount}, " +
                        "denominator: ${response.microGtuPerEuro.denominator}, " +
                        "numerator: ${response.microGtuPerEuro.numerator}")

                val rate = (sendTokenData.amount.multiply(response.microGtuPerEuro.denominator))
                    .divide(response.microGtuPerEuro.numerator)
                eurRateReady.postValue(rate)
            },
            failure = {
                handleBackendError(it)
            }
        )
    }

    private fun getTransferCostCCD() {
        proxyRepository.getTransferCost(
            type = ProxyRepository.SIMPLE_TRANSFER,
            memoSize = if (sendTokenData.memo == null) null else sendTokenData.memo!!.length / 2,
            success = {
                sendTokenData.energy = it.energy
                sendTokenData.fee = it.cost
                sendTokenData.account?.let { account ->
                    sendTokenData.max =
                        account.balanceAtDisposal - (sendTokenData.fee ?: BigInteger.ZERO)
                }
                waiting.postValue(false)
                feeReady.postValue(sendTokenData.fee)
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun getTransferCost(serializeTokenTransferParametersOutput: SerializeTokenTransferParametersOutput) {
        if (sendTokenData.account == null || sendTokenData.token == null) {
            errorInt.postValue(R.string.app_error_general)
            return
        }

        proxyRepository.getTransferCost(
            type = ProxyRepository.UPDATE,
            memoSize = null,
            amount = BigInteger.ZERO,
            sender = sendTokenData.account!!.address,
            contractIndex = sendTokenData.token!!.contractIndex.toInt(),
            contractSubindex = 0,
            receiveName = sendTokenData.token!!.contractName + ".transfer",
            parameter = serializeTokenTransferParametersOutput.parameter,
            success = {
                sendTokenData.energy = it.energy
                sendTokenData.fee = it.cost
                waiting.postValue(false)
                feeReady.postValue(sendTokenData.fee)
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private suspend fun getCCDDefaultToken(accountAddress: String): Token {
        val accountRepository =
            AccountRepository(App.appCore.session.walletStorage.database.accountDao())
        val account = accountRepository.findByAddress(accountAddress)
            ?: error("Account $accountAddress not found")
        return Token.ccd(account)
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        transactionWaiting.postValue(true)
        decryptAndContinue(password)
    }

    private suspend fun decryptAndContinue(password: String) {
        sendTokenData.account?.let { account ->
            val storageAccountDataEncrypted = account.encryptedAccountData
            if (storageAccountDataEncrypted == null) {
                errorInt.postValue(R.string.app_error_general)
                transactionWaiting.postValue(false)
                return
            }
            val decryptedJson = App.appCore.auth
                .decrypt(
                    password = password,
                    encryptedData = storageAccountDataEncrypted
                )
                ?.let(::String)

            if (decryptedJson != null) {
                val credentialsOutput =
                    App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
                getAccountEncryptedKey(credentialsOutput)
            } else {
                errorInt.postValue(R.string.app_error_encryption)
                transactionWaiting.postValue(false)
            }
        }
    }

    private suspend fun getAccountEncryptedKey(credentialsOutput: StorageAccountData) {
        proxyRepository.getAccountEncryptedKey(
            accountAddress = sendTokenData.receiver,
            success = {
                sendTokenData.receiverPublicKey = it.accountEncryptionKey
                sendTokenData.expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

                if (sendTokenData.token!!.isCcd)
                    viewModelScope.launch {
                        createTransactionCCD(
                            credentialsOutput.accountKeys,
                            credentialsOutput.encryptionSecretKey
                        )
                    }
                else
                    createTransaction(credentialsOutput.accountKeys)
            },
            failure = {
                transactionWaiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private suspend fun createTransactionCCD(keys: AccountData, encryptionSecretKey: String) {
        val toAddress = sendTokenData.receiver
        val nonce = sendTokenData.accountNonce
        val amount = sendTokenData.amount
        val energy = sendTokenData.energy
        val memo = sendTokenData.memo
        val accountId = sendTokenData.account?.id
        val balance = sendTokenData.accountBalance

        if (nonce == null || energy == null || accountId == null || balance == null) {
            errorInt.postValue(R.string.app_error_general)
            transactionWaiting.postValue(false)
            return
        }

        val transferInput = CreateTransferInput(
            sendTokenData.account!!.address,
            keys,
            toAddress,
            sendTokenData.expiry!!,
            amount.toString(),
            energy,
            nonce.nonce,
            memo,
            sendTokenData.globalParams,
            sendTokenData.receiverPublicKey,
            encryptionSecretKey,
            SendFundsViewModel.calculateInputEncryptedAmount(
                accountId = accountId,
                accountBalance = balance,
                transferRepository = transferRepository,
                accountUpdater = accountUpdater,
            ),
        )

        sendTokenData.createTransferInput = transferInput

        val output =
            App.appCore.cryptoLibrary.createTransfer(transferInput, CryptoLibrary.REGULAR_TRANSFER)

        if (output == null) {
            transactionWaiting.postValue(false)
            errorInt.postValue(R.string.app_error_general)
        } else {
            sendTokenData.createTransferOutput = output
            submitTransaction(output)
        }
    }

    private fun createTransaction(keys: AccountData) {
        if (sendTokenData.account == null || sendTokenData.token == null || sendTokenData.energy == null || sendTokenData.accountNonce == null) {
            errorInt.postValue(R.string.app_error_general)
            return
        }

        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

        viewModelScope.launch {
            val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(
                sendTokenData.token!!.token,
                sendTokenData.amount.toString(),
                sendTokenData.account!!.address,
                sendTokenData.receiver
            )
            val serializeTokenTransferParametersOutput =
                App.appCore.cryptoLibrary.serializeTokenTransferParameters(
                    serializeTokenTransferParametersInput
                )
            if (serializeTokenTransferParametersOutput == null) {
                errorInt.postValue(R.string.app_error_lib)
            } else {
                val payload = AccountTransactionPayload.Update(
                    address = ContractAddress(sendTokenData.token!!.contractIndex.toInt(), 0),
                    amount = BigInteger.ZERO,
                    maxEnergy = sendTokenData.energy!!,
                    maxContractExecutionEnergy = null,
                    message = serializeTokenTransferParametersOutput.parameter,
                    receiveName = sendTokenData.token!!.contractName + ".transfer"
                )
                val accountTransactionInput = CreateAccountTransactionInput(
                    expiry = expiry,
                    from = sendTokenData.account!!.address,
                    keys = keys,
                    nonce = sendTokenData.accountNonce!!.nonce,
                    payload = payload,
                    type = "Update"
                )
                val accountTransactionOutput =
                    App.appCore.cryptoLibrary.createAccountTransaction(accountTransactionInput)
                if (accountTransactionOutput == null) {
                    errorInt.postValue(R.string.app_error_lib)
                } else {
                    val createTransferOutput = CreateTransferOutput(
                        accountTransactionOutput.signatures,
                        accountTransactionOutput.transaction
                    )
                    submitTransaction(createTransferOutput)
                }
            }
        }
    }

    private fun submitTransaction(createTransferOutput: CreateTransferOutput) {
        submitTransaction?.dispose()
        submitTransaction = proxyRepository.submitTransfer(
            transfer = createTransferOutput,
            success = {
                println("LC -> submitTransaction SUCCESS = ${it.submissionId}")
                finishTransferCreation(it.submissionId)
                transactionWaiting.postValue(false)
            },
            failure = {
                println("LC -> submitTransaction ERROR ${it.stackTraceToString()}")
                handleBackendError(it)
                transactionWaiting.postValue(false)
            }
        )
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    private fun finishTransferCreation(submissionId: String) {
        val toAddress = sendTokenData.receiver
        val memo = sendTokenData.memo
        val expiry = sendTokenData.expiry
        val cost = sendTokenData.fee

        if (expiry == null || cost == null) {
            transactionWaiting.postValue(false)
            return
        }
        val createdAt = Date().time
        val isCCDTransfer = sendTokenData.token!!.isCcd

        val amount = if (isCCDTransfer) sendTokenData.amount else BigInteger.ZERO

        val transfer = Transfer(
            0,
            sendTokenData.account?.id ?: -1,
            amount,
            cost,
            sendTokenData.account?.address.orEmpty(),
            toAddress,
            expiry,
            memo,
            createdAt,
            submissionId,
            TransactionStatus.UNKNOWN,
            TransactionOutcome.UNKNOWN,
            getTransactionType(isCCDTransfer, memo != null),
            null,
            0,
            sendTokenData.accountNonce
        )
        transactionWaiting.postValue(false)
        saveNewTransfer(transfer)
        transaction.postValue(transfer.toTransaction())
        transactionReady.postValue(submissionId)
    }

    private fun getTransactionType(isCCDTransfer: Boolean, hasMemo: Boolean): TransactionType {
        if (!isCCDTransfer) {
            return TransactionType.UPDATE
        }
        if (hasMemo) {
            return TransactionType.TRANSFERWITHMEMO
        }
        return TransactionType.TRANSFER
    }

    private fun saveNewTransfer(transfer: Transfer) = viewModelScope.launch {
        transferRepository.insert(transfer)
    }
}
