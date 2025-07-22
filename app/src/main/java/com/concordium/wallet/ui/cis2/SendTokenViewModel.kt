package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.sdk.serializing.CborMapper
import com.concordium.sdk.transactions.Expiry
import com.concordium.sdk.transactions.TokenUpdate
import com.concordium.sdk.transactions.TransactionFactory
import com.concordium.sdk.transactions.TransactionSigner
import com.concordium.sdk.transactions.tokens.CborMemo
import com.concordium.sdk.transactions.tokens.TaggedTokenHolderAccount
import com.concordium.sdk.transactions.tokens.TokenOperationAmount
import com.concordium.sdk.transactions.tokens.TransferTokenOperation
import com.concordium.sdk.types.AccountAddress
import com.concordium.sdk.types.Nonce
import com.concordium.sdk.types.UInt64
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.price.TokenPriceRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.ContractAddress
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersInput
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersOutput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.data.model.SubmissionData
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
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Serializable
import java.math.BigInteger
import java.util.Date

data class SendTokenData(
    var token: NewToken? = null,
    var account: Account? = null,
    var amount: BigInteger = BigInteger.ZERO,
    /**
     * A valid address or an empty string.
     */
    var receiver: String = "",
    var receiverName: String? = null,
    var fee: BigInteger? = null,
    var max: BigInteger? = null,
    var memoHex: String? = null,
    var energy: Long? = null,
    var accountNonce: AccountNonce? = null,
    var expiry: Long? = null,
) : Serializable

class SendTokenViewModel(
    application: Application,
) : AndroidViewModel(application),
    KoinComponent {

    companion object {
        const val SEND_TOKEN_DATA = "SEND_TOKEN_DATA"
    }

    private val proxyRepository = ProxyRepository()
    private val tokenPriceRepository by inject<TokenPriceRepository>()
    private val transferRepository =
        TransferRepository(App.appCore.session.walletStorage.database.transferDao())
    private val accountUpdater = AccountUpdater(application, viewModelScope)
    private val sendFundsPreferences: WalletSendFundsPreferences =
        App.appCore.session.walletStorage.sendFundsPreferences

    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private var submitTransaction: BackendRequest<SubmissionData>? = null
    private var submitTransactionJob: Job? = null

    var sendTokenData = SendTokenData()
    val chooseToken: MutableLiveData<NewToken> by lazy { MutableLiveData<NewToken>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    val transactionReady: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val feeReady: MutableLiveData<BigInteger?> by lazy { MutableLiveData<BigInteger?>(null) }
    val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val showAuthentication: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transactionWaiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val transaction: MutableLiveData<Transaction> by lazy { MutableLiveData<Transaction>() }
    val eurRateReady: MutableLiveData<String?> by lazy { MutableLiveData<String?>() }

    val canSend: Boolean
        get() = with(sendTokenData) {
            token != null
                    && receiver.isNotBlank()
                    && amount.signum() > 0
                    && fee != null
                    && hasEnoughFunds()
        }
    val isHasShowReviewDialogAfterSendFunds: Boolean
        get() = App.appCore.setup.isHasShowReviewDialogAfterSendFunds

    init {
        chooseToken.observeForever { token ->
            sendTokenData.token = token
            sendTokenData.max = if (token is CCDToken) null else token.balance
            sendTokenData.fee = null
            sendTokenData.amount = BigInteger.ZERO
            feeReady.value = null
        }
    }

    fun dispose() {
        accountNonceRequest?.dispose()
        submitTransaction?.dispose()
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

    fun setMemoText(memoText: String?) {
        sendTokenData.memoHex = memoText
            ?.let(CborMapper.INSTANCE::writeValueAsBytes)
            ?.bytesToHex()
        loadTransactionFee()
    }

    fun getMemoText(): String? =
        sendTokenData
            .memoHex
            ?.hexToBytes()
            ?.let { CborMapper.INSTANCE.readValue(it, String::class.java) }

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
            if (sendTokenData.token.let { it != null && it !is CCDToken }) {
                loadTransactionFee()
            }
        } else {
            sendTokenData.receiver = ""
        }
    }

    fun onReceiverNameFound(name: String) {
        sendTokenData.receiverName = name
    }

    fun loadTransactionFee() {
        if (sendTokenData.token == null)
            return

        // TODO load the fee
//        if (sendTokenData.token!!.isCcd) {
//            waiting.postValue(true)
//            viewModelScope.launch {
//                getTransferCostCCD()
//            }
//            return
//        }
//
//        if (sendTokenData.account == null || sendTokenData.receiver.isEmpty()) {
//            return
//        }
//
//        viewModelScope.launch {
//            val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(
//                sendTokenData.token!!.token,
//                sendTokenData.amount.toString(),
//                sendTokenData.account!!.address,
//                sendTokenData.receiver
//            )
//            val serializeTokenTransferParametersOutput =
//                App.appCore.cryptoLibrary.serializeTokenTransferParameters(
//                    serializeTokenTransferParametersInput
//                )
//            if (serializeTokenTransferParametersOutput == null) {
//                waiting.postValue(false)
//                errorInt.postValue(R.string.app_error_lib)
//            } else {
//                getTransferCost(serializeTokenTransferParametersOutput)
//            }
//        }
    }

    fun hasEnoughFunds(): Boolean {
        if (sendTokenData.token == null)
            return false

        val ccdAtDisposal = sendTokenData.account?.balanceAtDisposal
            ?: BigInteger.ZERO

        return if (sendTokenData.token is CCDToken) {
            ccdAtDisposal >= sendTokenData.amount + (sendTokenData.fee ?: BigInteger.ZERO)
        } else {
            ccdAtDisposal >= (sendTokenData.fee ?: BigInteger.ZERO)
                    && sendTokenData.token!!.balance >= sendTokenData.amount
        }
    }

    fun setHasShowReviewDialogAfterSendFunds() {
        App.appCore.setup.setHasShowReviewDialogAfterSendFunds(true)
    }

    private fun getTransferCostCCD() {
        proxyRepository.getTransferCost(
            type = ProxyRepository.SIMPLE_TRANSFER,
            memoSize = sendTokenData.memoHex?.length?.div(2),
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

        // TODO Fix cis2 transfer cost
//        proxyRepository.getTransferCost(
//            type = ProxyRepository.UPDATE,
//            memoSize = null,
//            amount = BigInteger.ZERO,
//            sender = sendTokenData.account!!.address,
//            contractIndex = sendTokenData.token!!.contractIndex.toInt(),
//            contractSubindex = 0,
//            receiveName = sendTokenData.token!!.contractName + ".transfer",
//            parameter = serializeTokenTransferParametersOutput.parameter,
//            success = {
//                sendTokenData.energy = it.energy
//                sendTokenData.fee = it.cost
//                waiting.postValue(false)
//                feeReady.postValue(sendTokenData.fee)
//            },
//            failure = {
//                waiting.postValue(false)
//                handleBackendError(it)
//            }
//        )
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
                sendTokenData.expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

                when (val token = sendTokenData.token!!) {
                    is CCDToken -> {
                        viewModelScope.launch {
                            createTransactionCCD(credentialsOutput.accountKeys)
                        }
                    }

                    is NewContractToken -> {
                        createCis2Transaction(credentialsOutput.accountKeys, token)
                    }

                    is PLTToken -> {
                        createProtocolLevelTokenTransaction(credentialsOutput.accountKeys)
                    }
                }
            },
            failure = {
                transactionWaiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private suspend fun createTransactionCCD(keys: AccountData) {
        val toAddress = sendTokenData.receiver
        val nonce = sendTokenData.accountNonce
        val amount = sendTokenData.amount
        val energy = sendTokenData.energy
        val memoHex = sendTokenData.memoHex
        val accountId = sendTokenData.account?.id

        if (nonce == null || energy == null || accountId == null) {
            errorInt.postValue(R.string.app_error_general)
            transactionWaiting.postValue(false)
            return
        }

        // TODO use SDK for CCD transfer transaction
//        val transferInput = CreateTransferInput(
//            sendTokenData.account!!.address,
//            keys,
//            toAddress,
//            sendTokenData.expiry!!,
//            amount.toString(),
//            energy,
//            nonce.nonce,
//            memoHex,
//            sendTokenData.globalParams,
//            sendTokenData.receiverPublicKey,
//            encryptionSecretKey,
//            SendFundsViewModel.calculateInputEncryptedAmount(
//                accountId = accountId,
//                accountBalance = balance,
//                transferRepository = transferRepository,
//                accountUpdater = accountUpdater,
//            ),
//        )
//
//        sendTokenData.createTransferInput = transferInput
//
//        val output =
//            App.appCore.cryptoLibrary.createTransfer(transferInput, CryptoLibrary.REGULAR_TRANSFER)
//
//        if (output == null) {
//            transactionWaiting.postValue(false)
//            errorInt.postValue(R.string.app_error_general)
//        } else {
//            sendTokenData.createTransferOutput = output
//            submitTransaction(output)
//        }
    }

    private fun createCis2Transaction(
        keys: AccountData,
        token: NewContractToken,
    ) {
        if (sendTokenData.account == null || sendTokenData.token == null || sendTokenData.energy == null || sendTokenData.accountNonce == null) {
            errorInt.postValue(R.string.app_error_general)
            return
        }

        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

        viewModelScope.launch {
            val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(
                token.token,
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
                    address = ContractAddress(token.contractIndex.toInt(), 0),
                    amount = BigInteger.ZERO,
                    maxEnergy = sendTokenData.energy!!,
                    maxContractExecutionEnergy = null,
                    message = serializeTokenTransferParametersOutput.parameter,
                    receiveName = token.contractName + ".transfer"
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

    private fun createProtocolLevelTokenTransaction(
        keys: AccountData,
    ) = viewModelScope.launch {

        if (sendTokenData.account == null || sendTokenData.token == null || sendTokenData.energy == null || sendTokenData.accountNonce == null) {
            errorInt.postValue(R.string.app_error_general)
            return@launch
        }

        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

        val transaction = try {
            TransactionFactory.newTokenUpdate()
                .expiry(Expiry.from(expiry))
                .sender(AccountAddress.from(sendTokenData.account!!.address))
                .nonce(Nonce.from(sendTokenData.accountNonce!!.nonce.toLong()))
                .signer(TransactionSigner.from(keys.getSignerEntry()))
                .payload(
                    TokenUpdate.builder()
                        .tokenSymbol(TODO("token symbol"))
                        .operation(
                            TransferTokenOperation.builder()
                                .recipient(
                                    TaggedTokenHolderAccount(
                                        AccountAddress.from(sendTokenData.receiver)
                                    )
                                )
                                .amount(
                                    TokenOperationAmount(
                                        UInt64.from(sendTokenData.amount.toString()),
                                        TODO("token decimals"),
                                    )
                                )
                                .memo(
                                    sendTokenData
                                        .memoHex
                                        ?.hexToBytes()
                                        ?.let(CborMemo::from)
                                )
                                .build()
                        )
                        .build()
                )
                .build()

        } catch (e: Exception) {
            Log.e("Error creating transaction", e)
            transactionWaiting.postValue(false)
            errorInt.postValue(R.string.app_error_general)
            return@launch
        }

        submitTransaction(transaction)
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

    private fun submitTransaction(transaction: com.concordium.sdk.transactions.Transaction) {
        submitTransactionJob?.cancel()
        submitTransactionJob = viewModelScope.launch {
            try {
                val submissionId = proxyRepository
                    .submitSdkTransaction(transaction)
                    .submissionId
                println("LC -> submitTransaction SUCCESS = $submissionId")
                finishTransferCreation(submissionId)
                transactionWaiting.postValue(false)
            } catch (e: Exception) {
                println("LC -> submitTransaction ERROR ${e.stackTraceToString()}")
                handleBackendError(e)
                transactionWaiting.postValue(false)
            }
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    private fun finishTransferCreation(submissionId: String) {
        val toAddress = sendTokenData.receiver
        val memoHex = sendTokenData.memoHex
        val expiry = sendTokenData.expiry
        val cost = sendTokenData.fee

        if (expiry == null || cost == null) {
            transactionWaiting.postValue(false)
            return
        }
        val createdAt = Date().time

        val transfer = Transfer(
            id = 0,
            accountId = sendTokenData.account?.id ?: -1,
            amount = if (sendTokenData.token is CCDToken)
                sendTokenData.amount
            else
                BigInteger.ZERO,
            cost = cost,
            fromAddress = sendTokenData.account?.address.orEmpty(),
            toAddress = toAddress,
            expiry = expiry,
            memo = memoHex,
            createdAt = createdAt,
            submissionId = submissionId,
            transactionStatus = TransactionStatus.UNKNOWN,
            outcome = TransactionOutcome.UNKNOWN,
            transactionType = when (sendTokenData.token!!) {
                is CCDToken ->
                    if (memoHex != null)
                        TransactionType.TRANSFERWITHMEMO
                    else
                        TransactionType.TRANSFER

                is NewContractToken ->
                    TransactionType.UPDATE

                is PLTToken ->
                    TransactionType.TOKEN_UPDATE
            },
            newSelfEncryptedAmount = null,
            newStartIndex = 0,
            nonce = sendTokenData.accountNonce,
        )
        transactionWaiting.postValue(false)
        saveNewTransfer(transfer)
        transaction.postValue(transfer.toTransaction())
        transactionReady.postValue(submissionId)
    }

    private fun saveNewTransfer(transfer: Transfer) = viewModelScope.launch {
        transferRepository.insert(transfer)
    }
}
