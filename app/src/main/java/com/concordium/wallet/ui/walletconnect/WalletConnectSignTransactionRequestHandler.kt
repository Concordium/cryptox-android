package com.concordium.wallet.ui.walletconnect

import android.content.Context
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.ParameterToJsonInput
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.TransactionCost
import com.concordium.wallet.data.model.TransactionType
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
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WalletConnectSignTransactionRequestHandler(
    private val allowedAccountTransactionTypes: Set<TransactionType>,
    private val respondSuccess: (result: String) -> Unit,
    private val respondError: (message: String) -> Unit,
    private val emitEvent: (event: Event) -> Unit,
    private val emitState: (state: State) -> Unit,
    private val onFinish: () -> Unit,
    private val setIsSubmittingTransaction: (isSubmitting: Boolean) -> Unit,
    private val proxyRepository: ProxyRepository,
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
            AccountTransactionParams.fromSessionRequestParams(params).also { parsedParams ->
                check(parsedParams.type in allowedAccountTransactionTypes) {
                    "Transaction type '${parsedParams.type}' is not supported"
                }
            }
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

        val transactionPayload: AccountTransactionPayload? =
            transactionParams.parsePayload()
        if (transactionPayload == null) {
            Log.e(
                "failed_parsing_session_request_params_payload:" +
                        "\npayload=${transactionParams.payload}"
            )

            respondError("Failed parsing the request params payload")

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
                    estimatedFee = transactionCost.cost,
                    account = account,
                    canShowDetails = false,
                    isEnoughFunds = transactionPayload.amount + transactionCost.cost <= accountAtDisposalBalance,
                    appMetadata = appMetadata,
                )

            is AccountTransactionPayload.Update ->
                State.SessionRequestReview.TransactionRequestReview(
                    method = method,
                    receiver = transactionPayload.address.run { "$index, $subIndex" },
                    amount = transactionPayload.amount,
                    estimatedFee = transactionCost.cost,
                    account = account,
                    canShowDetails = true,
                    isEnoughFunds = transactionPayload.amount + transactionCost.cost <= accountAtDisposalBalance,
                    appMetadata = appMetadata,
                )
        }

        emitState(reviewState)
    }

    @Suppress("IfThenToElvis")
    private suspend fun getTransactionCost(): TransactionCost =
        when (val transactionPayload = this.transactionPayload) {
            is AccountTransactionPayload.Transfer ->
                getSimpleTransferCost()

            is AccountTransactionPayload.Update -> {
                val chainParameters = proxyRepository.getChainParameters()

                val maxEnergy: Long =
                    if (transactionPayload.maxEnergy != null) {
                        // Use the legacy total value if provided.
                        transactionPayload.maxEnergy
                    } else if (transactionPayload.maxContractExecutionEnergy != null) {
                        // Calculate the total value locally if only the execution energy provided.
                        TransactionEnergyCostCalculator.getContractTransactionMaxEnergy(
                            receiveName = transactionPayload.receiveName,
                            messageHex = transactionPayload.message,
                            maxContractExecutionEnergy = transactionPayload.maxContractExecutionEnergy,
                        )
                    } else {
                        error("The account transaction payload must contain either maxEnergy or maxContractExecutionEnergy")
                    }

                TransactionCost(
                    energy = maxEnergy,
                    euroPerEnergy = chainParameters.euroPerEnergy,
                    microGTUPerEuro = chainParameters.microGtuPerEuro,
                )
            }
        }

    private suspend fun getSimpleTransferCost(
    ): TransactionCost = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getTransferCost(
            type = ProxyRepository.SIMPLE_TRANSFER,
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

    suspend fun onAuthorizedForApproval(
        accountKeys: AccountData,
    ) {
        val accountTransactionPayload = this.transactionPayload

        val accountTransactionInput = CreateAccountTransactionInput(
            expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000,
            from = account.address,
            keys = accountKeys,
            nonce = transactionNonce.nonce,
            payload = accountTransactionPayload,
            type = when (accountTransactionPayload) {
                is AccountTransactionPayload.Transfer ->
                    "Transfer"

                is AccountTransactionPayload.Update ->
                    "Update"
            },
        )

        val accountTransactionOutput =
            App.appCore.cryptoLibrary.createAccountTransaction(accountTransactionInput)
        if (accountTransactionOutput == null) {
            Log.e("failed_creating_transaction")

            respondError("Failed creating transaction: crypto library internal error")

            emitEvent(
                Event.ShowFloatingError(
                    Error.CryptographyFailed
                )
            )
        } else {
            val createTransferOutput = CreateTransferOutput(
                transaction = accountTransactionOutput.transaction,
                signatures = accountTransactionOutput.signatures,
            )
            submitTransaction(createTransferOutput)
        }
    }

    private var submitTransactionRequest: BackendRequest<*>? = null
    private fun submitTransaction(createTransferOutput: CreateTransferOutput) {
        setIsSubmittingTransaction(true)

        submitTransactionRequest?.dispose()
        submitTransactionRequest = proxyRepository.submitTransfer(createTransferOutput,
            { submissionData ->
                Log.d(
                    "transaction_submitted:" +
                            "\nsubmissionId=${submissionData.submissionId}"
                )
                respondSuccess(Gson().toJson(TransactionSuccess(submissionData.submissionId)))

                setIsSubmittingTransaction(false)

                emitState(
                    State.TransactionSubmitted(
                        submissionId = submissionData.submissionId,
                        estimatedFee = transactionCost.cost,
                    )
                )
            },
            { error ->
                Log.e("failed_submitting_transaction", error)

                respondError("Failed submitting transaction: $error")

                setIsSubmittingTransaction(false)

                emitEvent(
                    Event.ShowFloatingError(
                        Error.TransactionSubmitFailed
                    )
                )
            }
        )
    }

    suspend fun onShowDetailsClicked() {
        val accountTransactionPayload = this.transactionPayload
        val accountTransactionParamsSchema = this.transactionParams.schema

        if (accountTransactionPayload is AccountTransactionPayload.Update) {
            val prettyPrintParams =
                if (accountTransactionParamsSchema != null)
                    App.appCore.cryptoLibrary
                        .parameterToJson(
                            ParameterToJsonInput(
                                parameter = accountTransactionPayload.message,
                                receiveName = accountTransactionPayload.receiveName,
                                schema = accountTransactionParamsSchema,
                                schemaVersion = accountTransactionParamsSchema.version,
                            )
                        )
                        ?.prettyPrint()
                        ?: context.getString(R.string.wallet_connect_error_transaction_request_stringify_params_failed)
                else if (accountTransactionPayload.message == "")
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
        } else {
            Log.w("Nothing to show as details for ${accountTransactionPayload::class.simpleName}")

            emitEvent(
                Event.ShowDetailsDialog(
                    title = getTransactionMethodName(transactionPayload),
                    prettyPrintDetails = context.getString(R.string.wallet_connect_transaction_request_no_details),
                )
            )
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
            is AccountTransactionPayload.Transfer ->
                context.getString(R.string.transaction_type_transfer)

            is AccountTransactionPayload.Update ->
                transactionPayload.receiveName
        }
}
