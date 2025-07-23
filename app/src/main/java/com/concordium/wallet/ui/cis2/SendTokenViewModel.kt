package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.sdk.serializing.CborMapper
import com.concordium.sdk.transactions.CCDAmount
import com.concordium.sdk.transactions.Expiry
import com.concordium.sdk.transactions.Memo
import com.concordium.sdk.transactions.TokenUpdate
import com.concordium.sdk.transactions.TransactionFactory
import com.concordium.sdk.transactions.TransactionSigner
import com.concordium.sdk.transactions.tokens.CborMemo
import com.concordium.sdk.transactions.tokens.TaggedTokenHolderAccount
import com.concordium.sdk.transactions.tokens.TokenOperation
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
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.data.model.SimpleFraction
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.preferences.WalletSendFundsPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.util.toTransaction
import com.concordium.wallet.data.walletconnect.AccountTransactionPayload
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.IOException
import org.koin.core.component.KoinComponent
import java.io.Serializable
import java.math.BigInteger
import java.util.Date

data class SendTokenData(
    val account: Account,
    var token: NewToken,
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
    var expiry: Long = 0L,
) : Serializable

class SendTokenViewModel(
    val sendTokenData: SendTokenData,
    private val tokenPriceRepository: TokenPriceRepository,
    application: Application,
) : AndroidViewModel(application),
    KoinComponent {

    private val proxyRepository = ProxyRepository()
    private val transferRepository =
        TransferRepository(App.appCore.session.walletStorage.database.transferDao())
    private val sendFundsPreferences: WalletSendFundsPreferences =
        App.appCore.session.walletStorage.sendFundsPreferences

    private var accountNonceRequest: BackendRequest<*>? = null
    private var submitTransaction: BackendRequest<*>? = null
    private var feeRequest: BackendRequest<*>? = null
    private var submitTransactionJob: Job? = null

    val chooseToken: MutableLiveData<NewToken> = MutableLiveData<NewToken>(sendTokenData.token)
    val waiting: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val transactionReady: MutableLiveData<String> = MutableLiveData<String>()
    val feeReady: MutableLiveData<BigInteger?> = MutableLiveData<BigInteger?>(null)
    val errorInt: MutableLiveData<Int> = MutableLiveData<Int>()
    val showAuthentication: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    val transactionWaiting: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    val transaction: MutableLiveData<Transaction> = MutableLiveData<Transaction>()
    val tokenEurRate: MutableLiveData<SimpleFraction?> = MutableLiveData()

    val canSend: Boolean
        get() = with(sendTokenData) {
            receiver.isNotBlank()
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

            loadEurRate(token)
        }
    }

    fun dispose() {
        accountNonceRequest?.dispose()
        submitTransaction?.dispose()
        feeRequest?.dispose()
    }

    fun send() {
        waiting.postValue(true)
        accountNonceRequest?.dispose()
        accountNonceRequest = proxyRepository.getAccountNonce(
            accountAddress = sendTokenData.account.address,
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
        loadFee()
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

            // Fee should be updated for non-CCD transfers.
            if (sendTokenData.token !is CCDToken) {
                loadFee()
            }
        } else {
            sendTokenData.receiver = ""
        }
    }

    fun onReceiverNameFound(name: String) {
        sendTokenData.receiverName = name
    }

    private var loadEurRateJob: Job? = null
    private fun loadEurRate(
        token: NewToken,
    ) {
        tokenEurRate.value = null

        loadEurRateJob?.cancel()
        loadEurRateJob = viewModelScope.launch {

            val rate = when (token) {
                is CCDToken ->
                    tokenPriceRepository
                        .getEurPerMicroCcd()
                        .getOrNull()

                is NewContractToken ->
                    null

                is PLTToken ->
                    null
            }

            tokenEurRate.postValue(rate)
        }
    }

    fun loadFee() = viewModelScope.launch {
        sendTokenData.fee = null
        feeReady.value = null

        when (val token = sendTokenData.token) {
            is CCDToken ->
                loadCcdTransferFee()

            is NewContractToken ->
                loadContractTokenTransferFee(token)

            is PLTToken ->
                loadProtocolLevelTokenTransferFee(token)
        }
    }

    fun hasEnoughFunds(): Boolean {
        val ccdAtDisposal = sendTokenData.account.balanceAtDisposal

        return if (sendTokenData.token is CCDToken) {
            ccdAtDisposal >= sendTokenData.amount + (sendTokenData.fee ?: BigInteger.ZERO)
        } else {
            ccdAtDisposal >= (sendTokenData.fee ?: BigInteger.ZERO)
                    && sendTokenData.token.balance >= sendTokenData.amount
        }
    }

    fun setHasShowReviewDialogAfterSendFunds() {
        App.appCore.setup.setHasShowReviewDialogAfterSendFunds(true)
    }

    private fun loadCcdTransferFee() {
        feeRequest?.dispose()
        feeRequest = proxyRepository.getTransferCost(
            type = ProxyRepository.SIMPLE_TRANSFER,
            memoSize = sendTokenData.memoHex?.length?.div(2),
            success = {
                sendTokenData.energy = it.energy
                sendTokenData.fee = it.cost
                sendTokenData.max =
                    sendTokenData.account.balanceAtDisposal - (sendTokenData.fee ?: BigInteger.ZERO)
                waiting.postValue(false)
                feeReady.postValue(sendTokenData.fee)
            },
            failure = {
                waiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private suspend fun loadContractTokenTransferFee(
        token: NewContractToken,
    ) {
        // Contract token transfer fee can't be loaded until the receiver is known.
        if (sendTokenData.receiver.isEmpty()) {
            return
        }

        val parameter: String? = getContractTokenTransferParameter(token)

        if (parameter == null) {
            waiting.postValue(false)
            errorInt.postValue(R.string.app_error_lib)
            return
        }

        feeRequest?.dispose()
        feeRequest = proxyRepository.getTransferCost(
            type = ProxyRepository.UPDATE,
            sender = token.accountAddress,
            contractIndex = token.contractIndex.toInt(),
            contractSubindex = token.subIndex.toInt(),
            receiveName = token.contractName + ".transfer",
            parameter = parameter,
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

    private fun loadProtocolLevelTokenTransferFee(
        token: PLTToken,
    ) {
        // Protocol level token transfer fee can't be loaded until the receiver is known.
        if (sendTokenData.receiver.isEmpty()) {
            return
        }

        val payload = getProtocolLevelTokenTransferPayload(token)

        feeRequest?.dispose()
        feeRequest = proxyRepository.getTransferCost(
            type = ProxyRepository.TOKEN_UPDATE,
            sender = token.accountAddress,
            tokenId = token.tokenId,
            listOperationsSize = CborMapper.INSTANCE.writeValueAsBytes(payload.operations).size,
            tokenOperationTypeCount = payload.operations
                .groupBy(TokenOperation::getType)
                .mapValues { it.value.size },
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

    fun continueWithPassword(password: String) = viewModelScope.launch {
        transactionWaiting.postValue(true)
        decryptAndContinue(password)
    }

    private suspend fun decryptAndContinue(password: String) {
        val storageAccountDataEncrypted = sendTokenData.account.encryptedAccountData
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

    private fun getAccountEncryptedKey(credentialsOutput: StorageAccountData) {
        proxyRepository.getAccountEncryptedKey(
            accountAddress = sendTokenData.receiver,
            success = {
                sendTokenData.expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

                when (val token = sendTokenData.token) {
                    is CCDToken ->
                        createCcdTransaction(
                            keys = credentialsOutput.accountKeys,
                        )

                    is NewContractToken ->
                        createContractTokenTransaction(
                            keys = credentialsOutput.accountKeys,
                            token = token,
                        )

                    is PLTToken ->
                        createProtocolLevelTokenTransaction(
                            keys = credentialsOutput.accountKeys,
                            token = token,
                        )
                }
            },
            failure = {
                transactionWaiting.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun createCcdTransaction(
        keys: AccountData,
    ) = viewModelScope.launch {

        val memoHex = sendTokenData.memoHex

        val transaction = try {
            if (memoHex != null)
                TransactionFactory.newTransferWithMemo()
                    .expiry(Expiry.from(sendTokenData.expiry))
                    .sender(AccountAddress.from(sendTokenData.account.address))
                    .nonce(Nonce.from(sendTokenData.accountNonce!!.nonce.toLong()))
                    .signer(TransactionSigner.from(keys.getSignerEntry()))
                    .amount(CCDAmount.fromMicro(sendTokenData.amount.toString()))
                    .receiver(AccountAddress.from(sendTokenData.receiver))
                    .memo(Memo.from(memoHex))
                    .build()
            else
                TransactionFactory.newTransfer()
                    .expiry(Expiry.from(sendTokenData.expiry))
                    .sender(AccountAddress.from(sendTokenData.account.address))
                    .nonce(Nonce.from(sendTokenData.accountNonce!!.nonce.toLong()))
                    .signer(TransactionSigner.from(keys.getSignerEntry()))
                    .amount(CCDAmount.fromMicro(sendTokenData.amount.toString()))
                    .receiver(AccountAddress.from(sendTokenData.receiver))
                    .build()
        } catch (e: Exception) {
            Log.e("Error creating transaction", e)
            transactionWaiting.postValue(false)
            errorInt.postValue(R.string.app_error_general)
            return@launch
        }

        submitTransaction(transaction)
    }

    private suspend fun getContractTokenTransferParameter(
        token: NewContractToken,
    ): String? {

        val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(
            tokenId = token.token,
            amount = sendTokenData.amount.toString(),
            from = token.accountAddress,
            to = sendTokenData.receiver
        )

        return App.appCore.cryptoLibrary.serializeTokenTransferParameters(
            serializeTokenTransferParametersInput
        )?.parameter
    }

    private fun createContractTokenTransaction(
        keys: AccountData,
        token: NewContractToken,
    ) = viewModelScope.launch {

        val parameter: String? = getContractTokenTransferParameter(token)
        if (parameter == null) {
            errorInt.postValue(R.string.app_error_lib)
            transactionWaiting.postValue(false)
            return@launch
        }

        val payload = AccountTransactionPayload.Update(
            address = ContractAddress(token.contractIndex.toInt(), token.subIndex.toInt()),
            amount = BigInteger.ZERO,
            maxEnergy = sendTokenData.energy!!,
            maxContractExecutionEnergy = null,
            message = parameter,
            receiveName = token.contractName + ".transfer"
        )

        val accountTransactionInput = CreateAccountTransactionInput(
            expiry = sendTokenData.expiry,
            from = sendTokenData.account.address,
            keys = keys,
            nonce = sendTokenData.accountNonce!!.nonce,
            payload = payload,
            type = "Update"
        )

        val accountTransactionOutput =
            App.appCore.cryptoLibrary.createAccountTransaction(accountTransactionInput)

        if (accountTransactionOutput == null) {
            errorInt.postValue(R.string.app_error_lib)
            transactionWaiting.postValue(false)
            return@launch
        }

        val createTransferOutput = CreateTransferOutput(
            accountTransactionOutput.signatures,
            accountTransactionOutput.transaction
        )

        submitTransaction(createTransferOutput)
    }

    private fun getProtocolLevelTokenTransferPayload(
        token: PLTToken,
    ): TokenUpdate =
        TokenUpdate.builder()
            .tokenSymbol(token.tokenId)
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
                            token.decimals,
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

    private fun createProtocolLevelTokenTransaction(
        keys: AccountData,
        token: PLTToken,
    ) = viewModelScope.launch {

        val transaction = try {
            TransactionFactory.newTokenUpdate()
                .expiry(Expiry.from(sendTokenData.expiry))
                .sender(AccountAddress.from(sendTokenData.account.address))
                .nonce(Nonce.from(sendTokenData.accountNonce!!.nonce.toLong()))
                .signer(TransactionSigner.from(keys.getSignerEntry()))
                .payload(getProtocolLevelTokenTransferPayload(token))
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
        if (throwable is IOException && throwable.message == "Canceled"){
            return
        }

        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    private fun finishTransferCreation(submissionId: String) {
        val memoHex = sendTokenData.memoHex
        val createdAt = Date().time

        val transfer = Transfer(
            id = 0,
            accountId = sendTokenData.account.id,
            amount = if (sendTokenData.token is CCDToken)
                sendTokenData.amount
            else
                BigInteger.ZERO,
            cost = sendTokenData.fee!!,
            fromAddress = sendTokenData.account.address,
            toAddress = sendTokenData.receiver,
            expiry = sendTokenData.expiry,
            memo = memoHex,
            createdAt = createdAt,
            submissionId = submissionId,
            transactionStatus = TransactionStatus.UNKNOWN,
            outcome = TransactionOutcome.UNKNOWN,
            transactionType = when (sendTokenData.token) {
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
