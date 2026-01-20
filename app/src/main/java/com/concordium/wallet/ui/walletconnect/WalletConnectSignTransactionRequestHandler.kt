package com.concordium.wallet.ui.walletconnect

import android.content.Context
import com.concordium.sdk.transactions.CCDAmount
import com.concordium.sdk.transactions.Expiry
import com.concordium.sdk.transactions.Parameter
import com.concordium.sdk.transactions.ReceiveName
import com.concordium.sdk.transactions.SignerEntry
import com.concordium.sdk.transactions.TokenUpdate
import com.concordium.sdk.transactions.TransactionFactory
import com.concordium.sdk.transactions.TransactionSigner
import com.concordium.sdk.transactions.Transfer
import com.concordium.sdk.transactions.UpdateContract
import com.concordium.sdk.transactions.tokens.TokenOperation
import com.concordium.sdk.types.AccountAddress
import com.concordium.sdk.types.ContractAddress
import com.concordium.sdk.types.Nonce
import com.concordium.sdk.types.UInt64
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.ParameterToJsonInput
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TransactionCost
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.TransactionEnergyCostCalculator
import com.concordium.wallet.data.walletconnect.AccountTransactionParams
import com.concordium.wallet.data.walletconnect.AccountTransactionPayload
import com.concordium.wallet.data.walletconnect.TransactionSuccess
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Error
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Event
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.State
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PrettyPrint.prettyPrint
import com.reown.util.hexToBytes
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.jvm.optionals.getOrNull

class WalletConnectSignTransactionRequestHandler(
    private val respondSuccess: (result: String) -> Unit,
    private val respondError: (message: String) -> Unit,
    private val emitEvent: (event: Event) -> Unit,
    private val emitState: (state: State) -> Unit,
    private val onFinish: () -> Unit,
    private val setIsSubmittingTransaction: (isSubmitting: Boolean) -> Unit,
    private val proxyRepository: ProxyRepository,
    private val tokensInteractor: TokensInteractor,
    private val context: Context,
) {
    private lateinit var account: Account
    private lateinit var transactionParams: AccountTransactionParams
    private lateinit var transactionPayload: AccountTransactionPayload
    private lateinit var transactionCost: TransactionCost
    private lateinit var transactionNonce: AccountNonce

    suspend fun start(
        params: String,
        account: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        val transactionParams: AccountTransactionParams = try {
            AccountTransactionParams.fromSessionRequestParams(params)
        } catch (error: Exception) {
            Log.e(
                "failed_parsing_session_request_params:" +
                        "\nparams=$params",
                error
            )

            respondError("Failed parse the request: $error")

            emitEvent(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )

            onFinish()

            return
        }

        val transactionPayload: AccountTransactionPayload = try {
            transactionParams.parsePayload()
        } catch (error: Exception) {
            Log.e(
                "failed_parsing_session_request_params_payload:" +
                        "\npayload=${transactionParams.payload}",
                error
            )

            respondError("Failed parsing the request params payload: $error")

            emitEvent(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )

            onFinish()

            return
        }

        val senderAccountAddress = transactionParams.sender
        if (senderAccountAddress != account.address) {
            Log.e("Received a request to sign an account transaction for a different account than the one connected in the session: $senderAccountAddress")

            respondError("The request was for a different account than the one connected.")

            emitEvent(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )

            onFinish()

            return
        }

        this.account = account
        this.transactionParams = transactionParams
        this.transactionPayload = transactionPayload

        loadDataAndPresentReview(appMetadata)
    }

    private suspend fun loadDataAndPresentReview(
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        val accountNonce: AccountNonce
        val transactionCost: TransactionCost
        val token: Token

        Log.d("loading_data")

        try {
            accountNonce = try {
                getAccountNonce(account)
            } catch (error: Exception) {
                Log.e("failed_loading_transaction_nonce", error)
                throw error
            }

            transactionCost = try {
                getTransactionCost()
            } catch (error: Exception) {
                Log.e("failed_loading_transaction_cost", error)
                throw error
            }

            token = try {
                when (val transactionPayload = this.transactionPayload) {
                    is AccountTransactionPayload.Transfer,
                    is AccountTransactionPayload.Update,
                    ->
                        CCDToken(
                            account = account,
                            withTotalBalance = true,
                        )

                    is AccountTransactionPayload.PltTransfer ->
                        tokensInteractor
                            .loadTokens(
                                accountAddress = account.address,
                                loadBalances = false,
                                onlyTransferable = true,
                                addCCDToken = false,
                            )
                            .getOrThrow()
                            .firstOrNull {
                                it is ProtocolLevelToken
                                        && it.tokenId == transactionPayload.tokenId
                            }
                            ?: error("Missing the requested token ${transactionPayload.tokenId}")
                }
            } catch (error: Exception) {
                Log.e("failed_loading_token", error)
                throw error
            }
        } catch (error: Exception) {
            respondError("Failed loading transaction data: $error")

            emitEvent(
                Event.ShowFloatingError(
                    Error.LoadingFailed
                )
            )

            onFinish()

            return
        }

        Log.d(
            "data_loaded:" +
                    "\ncost=$transactionCost," +
                    "\nnonce=$accountNonce"
        )

        val method = getTransactionMethodName(transactionPayload)
        val accountAtDisposalBalance = account.balanceAtDisposal
        this.transactionCost = transactionCost
        this.transactionNonce = accountNonce

        val reviewState = when (val transactionPayload = this.transactionPayload) {
            is AccountTransactionPayload.Transfer ->
                State.SessionRequestReview.TransactionRequestReview(
                    method = method,
                    receiver = transactionPayload.toAddress,
                    amount = transactionPayload.amount,
                    token = token,
                    estimatedFee = transactionCost.cost,
                    account = account,
                    canShowDetails = false,
                    isEnoughFunds = transactionPayload.amount + transactionCost.cost <= accountAtDisposalBalance,
                    sponsor = null,
                    appMetadata = appMetadata,
                )

            is AccountTransactionPayload.Update ->
                State.SessionRequestReview.TransactionRequestReview(
                    method = method,
                    receiver = transactionPayload.address.run { "$index, $subIndex" },
                    amount = transactionPayload.amount,
                    token = token,
                    estimatedFee = transactionCost.cost,
                    account = account,
                    canShowDetails = true,
                    isEnoughFunds = transactionPayload.amount + transactionCost.cost <= accountAtDisposalBalance,
                    sponsor = null,
                    appMetadata = appMetadata,
                )

            is AccountTransactionPayload.PltTransfer -> {
                val tokenAmount =
                    BigInteger(1, transactionPayload.transfer.amount.value.bytes)

                State.SessionRequestReview.TransactionRequestReview(
                    method = method,
                    receiver = transactionPayload.transfer.recipient.address.encoded(),
                    amount = tokenAmount,
                    token = token,
                    estimatedFee = transactionCost.cost,
                    account = account,
                    canShowDetails = transactionPayload.transfer.memo.isPresent,
                    isEnoughFunds = transactionCost.cost <= accountAtDisposalBalance
                            && tokenAmount <= token.balance,
                    sponsor = null,
                    appMetadata = appMetadata,
                )
            }
        }

        emitState(reviewState)
    }

    private suspend fun getTransactionCost(): TransactionCost =
        when (val transactionPayload = this.transactionPayload) {
            is AccountTransactionPayload.Transfer ->
                getTransferCost()

            is AccountTransactionPayload.Update ->
                getContractUpdateCost(
                    updatePayload = transactionPayload,
                )

            is AccountTransactionPayload.PltTransfer -> getProtocolLevelTokenTransferCost(
                pltTransferPayload = transactionPayload,
            )
        }

    private suspend fun getTransferCost(
    ): TransactionCost = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getTransferCost(
            type = ProxyRepository.SIMPLE_TRANSFER,
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    @Suppress("IfThenToElvis")
    private suspend fun getContractUpdateCost(
        updatePayload: AccountTransactionPayload.Update,
    ): TransactionCost {
        val chainParameters = proxyRepository.getChainParameters()

        val maxEnergy: Long =
            if (updatePayload.maxEnergy != null) {
                // Use the legacy total value if provided.
                updatePayload.maxEnergy
            } else if (updatePayload.maxContractExecutionEnergy != null) {
                // Calculate the total value locally if only the execution energy provided.
                TransactionEnergyCostCalculator.getContractTransactionMaxEnergy(
                    receiveName = updatePayload.receiveName,
                    messageHex = updatePayload.message,
                    maxContractExecutionEnergy = updatePayload.maxContractExecutionEnergy,
                )
            } else {
                error("The account transaction payload must contain either maxEnergy or maxContractExecutionEnergy")
            }

        return TransactionCost(
            energy = maxEnergy,
            euroPerEnergy = chainParameters.euroPerEnergy,
            microGTUPerEuro = chainParameters.microGtuPerEuro,
        )
    }

    private suspend fun getProtocolLevelTokenTransferCost(
        pltTransferPayload: AccountTransactionPayload.PltTransfer,
    ): TransactionCost = suspendCancellableCoroutine { continuation ->
        val tokenUpdatePayload = getProtocolLevelTokenTransferPayload(pltTransferPayload)
        val backendRequest = proxyRepository.getTransferCost(
            type = ProxyRepository.TOKEN_UPDATE,
            sender = account.address,
            tokenId = tokenUpdatePayload.tokenSymbol,
            listOperationsSize = tokenUpdatePayload.operationsSerialized.size,
            tokenOperationTypeCount = tokenUpdatePayload.operations
                .groupBy(TokenOperation::getType)
                .mapValues { it.value.size },
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private suspend fun getAccountNonce(
        account: Account,
    ): AccountNonce = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getAccountNonce(
            accountAddress = account.address,
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private fun getProtocolLevelTokenTransferPayload(
        transactionPayload: AccountTransactionPayload.PltTransfer,
    ): TokenUpdate =
        TokenUpdate.builder()
            .tokenSymbol(transactionPayload.tokenId)
            .operation(transactionPayload.transfer)
            .build()

    private fun getTransactionExpiry(): Expiry =
        Expiry.from(DateTimeUtil.nowPlusMinutes(10))

    suspend fun onAuthorizedForApproval(
        accountKeys: AccountData,
    ) {
        setIsSubmittingTransaction(true)

        try {
            val submissionId = when (val transactionPayload = this.transactionPayload) {
                is AccountTransactionPayload.Transfer ->
                    submitTransferTransaction(
                        transferPayload = transactionPayload,
                        signer = accountKeys.getSignerEntry(),
                    )

                is AccountTransactionPayload.Update ->
                    submitContractUpdateTransaction(
                        updatePayload = transactionPayload,
                        signer = accountKeys.getSignerEntry(),
                    )

                is AccountTransactionPayload.PltTransfer ->
                    submitProtocolLevelTokenTransferTransaction(
                        pltTransferPayload = transactionPayload,
                        signer = accountKeys.getSignerEntry(),
                    )
            }

            respondSuccess(App.appCore.gson.toJson(TransactionSuccess(submissionId)))

            emitState(
                State.TransactionSubmitted(
                    submissionId = submissionId,
                    estimatedFee = transactionCost.cost,
                    isSponsored = false,
                )
            )
        } catch (error: Exception) {
            Log.e("failed_submitting_transaction", error)

            respondError("Failed submitting transaction: $error")

            setIsSubmittingTransaction(false)

            emitEvent(
                Event.ShowFloatingError(
                    Error.TransactionSubmitFailed
                )
            )
        } finally {
            setIsSubmittingTransaction(false)
        }
    }

    private suspend fun submitTransferTransaction(
        transferPayload: AccountTransactionPayload.Transfer,
        signer: SignerEntry,
    ): String =
        submitTransaction(
            TransactionFactory
                .newTransfer(
                    Transfer
                        .builder()
                        .amount(CCDAmount.fromMicro(transferPayload.amount.toString()))
                        .receiver(AccountAddress.from(transferPayload.toAddress))
                        .build()
                )
                .expiry(getTransactionExpiry())
                .sender(AccountAddress.from(account.address))
                .nonce(Nonce.from(transactionNonce.nonce.toLong()))
                .sign(TransactionSigner.from(signer))
        )

    private suspend fun submitContractUpdateTransaction(
        updatePayload: AccountTransactionPayload.Update,
        signer: SignerEntry,
    ): String =
        submitTransaction(
            TransactionFactory
                .newUpdateContract(
                    UpdateContract.from(
                        CCDAmount.fromMicro(updatePayload.amount.toString()),
                        ContractAddress(
                            updatePayload.address.subIndex.toLong(),
                            updatePayload.address.index.toLong()
                        ),
                        ReceiveName.parse(updatePayload.receiveName),
                        Parameter.from(updatePayload.message.hexToBytes())
                    ),
                    UInt64.from(transactionCost.energy)
                )
                .expiry(getTransactionExpiry())
                .sender(AccountAddress.from(account.address))
                .nonce(Nonce.from(transactionNonce.nonce.toLong()))
                .sign(TransactionSigner.from(signer))
        )

    private suspend fun submitProtocolLevelTokenTransferTransaction(
        pltTransferPayload: AccountTransactionPayload.PltTransfer,
        signer: SignerEntry,
    ): String =
        submitTransaction(
            TransactionFactory
                .newTokenUpdate(getProtocolLevelTokenTransferPayload(pltTransferPayload))
                .expiry(getTransactionExpiry())
                .sender(AccountAddress.from(account.address))
                .nonce(Nonce.from(transactionNonce.nonce.toLong()))
                .sign(TransactionSigner.from(signer))
        )

    private suspend fun submitTransaction(
        transaction: com.concordium.sdk.transactions.Transaction,
    ): String {
        return proxyRepository
            .submitSdkTransaction(transaction)
            .submissionId
    }

    suspend fun onShowDetailsClicked() {

        when (val transactionPayload = this.transactionPayload) {
            is AccountTransactionPayload.Update -> {
                val transactionParamsSchema = this.transactionParams.schema
                val prettyPrintParams =
                    if (transactionParamsSchema != null)
                        App.appCore.cryptoLibrary
                            .parameterToJson(
                                ParameterToJsonInput(
                                    parameter = transactionPayload.message,
                                    receiveName = transactionPayload.receiveName,
                                    schema = transactionParamsSchema,
                                    schemaVersion = transactionParamsSchema.version,
                                )
                            )
                            ?.prettyPrint()
                            ?: context.getString(R.string.wallet_connect_error_transaction_request_stringify_params_failed)
                    else if (transactionPayload.message == "")
                        context.getString(R.string.wallet_connect_transaction_request_no_parameters)
                    else
                        context.getString(R.string.wallet_connect_error_transaction_request_stringify_params_no_schema)

                emitEvent(
                    Event.ShowDetailsDialog(
                        title = getTransactionMethodName(transactionPayload),
                        prettyPrintDetails = context.getString(
                            R.string.wallet_connect_template_transaction_request_details,
                            transactionCost.energy.toString(),
                            prettyPrintParams,
                        ),
                    )
                )
            }

            is AccountTransactionPayload.PltTransfer -> {
                emitEvent(
                    Event.ShowDetailsDialog(
                        title = getTransactionMethodName(transactionPayload),
                        prettyPrintDetails = context.getString(
                            R.string.wallet_connect_template_plt_transfer_request_details,
                            transactionPayload.transfer.memo.getOrNull().toString(),
                        ),
                    )
                )
            }

            is AccountTransactionPayload.Transfer -> {
                Log.w("Nothing to show as details for ${transactionPayload::class.simpleName}")

                emitEvent(
                    Event.ShowDetailsDialog(
                        title = getTransactionMethodName(transactionPayload),
                        prettyPrintDetails = context.getString(R.string.wallet_connect_transaction_request_no_details),
                    )
                )
            }
        }
    }

    fun onTransactionSubmittedFinishClicked() {
        onFinish()
    }

    fun onTransactionSubmittedViewClosed() {
        onFinish()
    }

    private fun getTransactionMethodName(transactionPayload: AccountTransactionPayload) =
        when (transactionPayload) {
            is AccountTransactionPayload.Transfer,
            is AccountTransactionPayload.PltTransfer,
            ->
                context.getString(R.string.transaction_type_transfer)

            is AccountTransactionPayload.Update ->
                transactionPayload.receiveName
        }

    companion object {
        const val METHOD = "sign_and_send_transaction"
    }
}
