package com.concordium.wallet.ui.cis2.send

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.sdk.serializing.CborMapper
import com.concordium.sdk.transactions.TokenUpdate
import com.concordium.sdk.transactions.tokens.CborMemo
import com.concordium.sdk.transactions.tokens.TaggedTokenHolderAccount
import com.concordium.sdk.transactions.tokens.TokenOperation
import com.concordium.sdk.transactions.tokens.TokenOperationAmount
import com.concordium.sdk.transactions.tokens.TransferTokenOperation
import com.concordium.sdk.types.AccountAddress
import com.concordium.sdk.types.UInt64
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.backend.price.TokenPriceRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersInput
import com.concordium.wallet.data.model.AccountBalanceInfo
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.SimpleFraction
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenAccountStateList
import com.concordium.wallet.data.preferences.WalletSendFundsPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import com.reown.util.bytesToHex
import com.reown.util.hexToBytes
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import okio.IOException
import org.koin.core.component.KoinComponent
import java.math.BigInteger

class SendTokenViewModel(
    private val tokenPriceRepository: TokenPriceRepository,
    mainViewModel: MainViewModel,
    application: Application,
) : AndroidViewModel(application),
    KoinComponent {

    private val proxyRepository = ProxyRepository()
    private val sendFundsPreferences: WalletSendFundsPreferences =
        App.appCore.session.walletStorage.sendFundsPreferences

    private var accountNonceRequest: BackendRequest<*>? = null
    private var submitTransaction: BackendRequest<*>? = null
    private var feeRequest: BackendRequest<*>? = null
    lateinit var sendTokenData: SendTokenData

    val token: MutableLiveData<Token> = MutableLiveData<Token>()
    val waiting: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val feeReady: MutableLiveData<BigInteger?> = MutableLiveData<BigInteger?>(null)
    val errorInt: MutableLiveData<Int> = MutableLiveData<Int>()
    val tokenEurRate: MutableLiveData<SimpleFraction?> = MutableLiveData()
    private val _accountUpdated = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val accountUpdated = _accountUpdated.asSharedFlow()
    private val _recipientError = MutableStateFlow(-1)
    val recipientError = _recipientError.asStateFlow()
    private val _hasEnoughFunds = MutableStateFlow(true)
    val hasEnoughFunds = _hasEnoughFunds.asStateFlow()

    val canSend: Boolean
        get() = with(sendTokenData) {
            receiverAddress != null
                    && amount.signum() > 0
                    && fee != null
                    && hasEnoughFunds.value
        } && recipientError.value == -1

    init {
        viewModelScope.launch {
            mainViewModel.activeAccount
                .distinctUntilChangedBy { it?.address }
                .collect {
                    it?.let(::updateAccount)
                }
        }

        feeReady.observeForever {
            checkIfEnoughFunds()
        }

        if (sendTokenData.fee == null) {
            loadFee()
        }
    }

    private fun updateAccount(account: Account) {
        val ccdToken = CCDToken(
            account = account,
        )
        sendTokenData = SendTokenData(
            account = account,
            token = ccdToken
        )
        onTokenSelected(ccdToken)
        _accountUpdated.tryEmit(true)
    }

    override fun onCleared() {
        super.onCleared()
        accountNonceRequest?.dispose()
        submitTransaction?.dispose()
        feeRequest?.dispose()
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
            sendTokenData.receiverAddress = input

            // Fee should be updated for non-CCD transfers.
            if (sendTokenData.token !is CCDToken) {
                loadFee()
            }
        } else {
            sendTokenData.receiverAddress = null
        }
        checkRecipientState()
    }

    fun onReceiverNameFound(name: String) {
        sendTokenData.receiverName = name
    }

    fun onTokenSelected(token: Token) {
        sendTokenData.token = token
        sendTokenData.maxAmount = if (token is CCDToken) null else token.balance
        if (token is ContractToken) {
            sendTokenData.memoHex = null
        }
        this.token.value = token

        loadFee()
        loadEurRate(token)
        checkRecipientState()
        checkIfEnoughFunds()
    }

    private var loadEurRateJob: Job? = null
    private fun loadEurRate(
        token: Token,
    ) {
        tokenEurRate.value = null

        loadEurRateJob?.cancel()
        loadEurRateJob = viewModelScope.launch {

            val rate = when (token) {
                is CCDToken ->
                    tokenPriceRepository
                        .getEurPerMicroCcd()
                        .getOrNull()

                is ContractToken -> null
                is ProtocolLevelToken -> null
            }

            tokenEurRate.postValue(rate)
        }
    }

    fun loadFee() = viewModelScope.launch {
        sendTokenData.fee = null
        feeReady.value = null

        when (val token = sendTokenData.token) {
            is CCDToken -> loadCcdTransferFee()
            is ContractToken -> loadContractTokenTransferFee(token)
            is ProtocolLevelToken -> loadProtocolLevelTokenTransferFee(token)
        }
    }

    private fun checkIfEnoughFunds() {
        val ccdAtDisposal = sendTokenData.account.balanceAtDisposal

        _hasEnoughFunds.tryEmit(
            if (sendTokenData.token is CCDToken) {
                ccdAtDisposal >= sendTokenData.amount + (sendTokenData.fee ?: BigInteger.ZERO)
            } else {
                ccdAtDisposal >= (sendTokenData.fee ?: BigInteger.ZERO)
                        && sendTokenData.token.balance >= sendTokenData.amount
            }
        )
    }

    private fun loadCcdTransferFee() {
        feeRequest?.dispose()
        feeRequest = proxyRepository.getTransferCost(
            type = ProxyRepository.SIMPLE_TRANSFER,
            memoSize = sendTokenData.memoHex?.length?.div(2),
            success = {
                sendTokenData.maxEnergy = it.energy
                sendTokenData.fee = it.cost
                sendTokenData.maxAmount =
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
        token: ContractToken,
    ) {
        // Contract token transfer fee can't be loaded until the receiver is known.
        if (sendTokenData.receiverAddress == null) {
            return
        }

        val parameter: String? = getContractTokenTransferParameterHex(token)

        if (parameter == null) {
            waiting.postValue(false)
            errorInt.postValue(R.string.app_error_lib)
            return
        }

        feeRequest?.dispose()
        feeRequest = proxyRepository.getTransferCost(
            amount = BigInteger.ZERO,
            type = ProxyRepository.UPDATE,
            sender = token.accountAddress,
            contractIndex = token.contractIndex.toInt(),
            contractSubindex = token.subIndex.toInt(),
            receiveName = token.contractName + ".transfer",
            parameter = parameter,
            success = {
                sendTokenData.maxEnergy = it.energy
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
        token: ProtocolLevelToken,
    ) {
        // Protocol level token transfer fee can't be loaded until the receiver is known.
        if (sendTokenData.receiverAddress == null) {
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
                sendTokenData.maxEnergy = it.energy
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

    private suspend fun getContractTokenTransferParameterHex(
        token: ContractToken,
    ): String? {

        val serializeTokenTransferParametersInput = SerializeTokenTransferParametersInput(
            tokenId = token.token,
            amount = sendTokenData.amount.toString(),
            from = token.accountAddress,
            to = sendTokenData.receiverAddress!!,
        )

        return App.appCore.cryptoLibrary.serializeTokenTransferParameters(
            serializeTokenTransferParametersInput
        )?.parameter
    }

    private fun getProtocolLevelTokenTransferPayload(
        token: ProtocolLevelToken,
    ): TokenUpdate =
        TokenUpdate.builder()
            .tokenSymbol(token.tokenId)
            .operation(
                TransferTokenOperation.builder()
                    .recipient(
                        TaggedTokenHolderAccount(
                            AccountAddress.from(sendTokenData.receiverAddress)
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

    private fun handleBackendError(throwable: Throwable) {
        if (throwable is IOException && throwable.message == "Canceled") {
            return
        }

        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    private fun checkRecipientState() = viewModelScope.launch {
        val token = sendTokenData.token
        _recipientError.value = -1

        if (sendTokenData.receiverAddress == null)
            return@launch

        val recipientBalanceInfo: AccountBalanceInfo? = try {
            proxyRepository
                .getAccountBalanceSuspended(
                    accountAddress = sendTokenData.receiverAddress!!,
                )
                .finalizedBalance
        } catch (e: Exception) {
            ensureActive()
            handleBackendError(e)
            return@launch
        }

        // Check if the recipient even exists.
        // Sending to non-existing accounts burns the fee.
        if (recipientBalanceInfo == null) {
            _recipientError.value = R.string.app_error_backend_account_does_not_exist
            return@launch
        }

        // Check if the recipient can receive the protocol level token.
        // Sending to banned or not allowed accounts burns the fee.
        if (token is ProtocolLevelToken) {
            val recipientTokenAccountState: TokenAccountStateList? =
                recipientBalanceInfo
                    .accountTokens
                    ?.find { it.token.tokenId == token.tokenId }
                    ?.tokenAccountState
                    ?.state

            if (recipientTokenAccountState?.denyList == true) {
                _recipientError.value = R.string.cis_error_recipient_in_deny_list
                return@launch
            } else if (token.isInAllowList == true
                && recipientTokenAccountState?.allowList != true
            ) {
                _recipientError.value = R.string.cis_error_recipient_not_in_allow_list
                return@launch
            }
        }
    }
}
