package com.concordium.wallet.ui.walletconnect

import android.content.Context
import com.concordium.sdk.serializing.JsonMapper
import com.concordium.sdk.transactions.PartiallySignedSponsoredTransaction
import com.concordium.sdk.transactions.Payload
import com.concordium.sdk.transactions.SignerEntry
import com.concordium.sdk.transactions.TokenUpdate
import com.concordium.sdk.transactions.TransactionFactory
import com.concordium.sdk.transactions.TransactionSigner
import com.concordium.sdk.transactions.Transfer
import com.concordium.sdk.transactions.UpdateContract
import com.concordium.sdk.transactions.tokens.TransferTokenOperation
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.ParameterToJsonInput
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.walletconnect.AccountTransactionParams
import com.concordium.wallet.data.walletconnect.AccountTransactionPayload
import com.concordium.wallet.data.walletconnect.TransactionSuccess
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Error
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Event
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.State
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.PrettyPrint.prettyPrint
import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.jvm.optionals.getOrNull

class WalletConnectSignSponsoredTransactionRequestHandler(
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
    private lateinit var partiallySignedTransaction: PartiallySignedSponsoredTransaction
    private lateinit var transactionPayload: AccountTransactionPayload

    suspend fun start(
        params: String,
        account: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        val partiallySignedTransaction: PartiallySignedSponsoredTransaction = try {
            JsonMapper.INSTANCE.readValue(
                params,
                PartiallySignedSponsoredTransaction::class.java
            ).also {
                check(
                    it.header.sponsor.isPresent
                            && it.sponsorSignature.isPresent
                ) {
                    "The wallet only supports transactions signed by a sponsor"
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

        val transactionPayload: AccountTransactionPayload = try {
            val payload =
                Payload.fromBytes(ByteBuffer.wrap(partiallySignedTransaction.payload.rawBytes))

            when (payload) {
                is Transfer ->
                    AccountTransactionPayload.Transfer(
                        amount = BigInteger(1, payload.amount.value.bytes),
                        toAddress = payload.receiver.toString(),
                    )

                is TokenUpdate -> {
                    AccountTransactionPayload.PltTransfer(
                        tokenId = payload.tokenSymbol,
                        transfer =
                            payload
                                .operations
                                .takeIf { it.size == 1 }
                                ?.let { it[0] as? TransferTokenOperation }
                                ?: error("The wallet only supports a single token transfer"),
                    )
                }

                is UpdateContract -> {
                    AccountTransactionPayload.Update(
                        address = payload.contractAddress,
                        amount = BigInteger(1, payload.amount.value.bytes),
                        maxEnergy = null,
                        maxContractExecutionEnergy = null,
                        message = payload.param.bytes.toHexString(),
                        receiveName = payload.receiveName.toString(),
                    )
                }

                else ->
                    error("The wallet only supports CCD and token transfers")
            }
        } catch (error: Exception) {
            Log.e(
                "failed_parsing_payload",
                error
            )

            respondError("Failed parsing the payload: $error")

            emitEvent(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )

            onFinish()

            return
        }

        val senderAccountAddress = partiallySignedTransaction.header.sender.toString()
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
        this.partiallySignedTransaction = partiallySignedTransaction
        this.transactionPayload = transactionPayload

        loadDataAndPresentReview(appMetadata)
    }

    private suspend fun loadDataAndPresentReview(
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        val token: Token

        Log.d("loading_data")

        try {
            token = try {
                when (val transactionPayload = this.transactionPayload) {
                    is AccountTransactionPayload.Transfer
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

                    is AccountTransactionPayload.Update ->
                        tokensInteractor
                            .loadTokens(
                                accountAddress = account.address,
                                loadBalances = true,
                                onlyTransferable = true,
                                addCCDToken = false,
                            )
                            .getOrThrow()
                            .firstOrNull {
                                it is ContractToken &&
                                        it.run { "$contractIndex, $subIndex" } ==
                                        transactionPayload.address.run { "$index, $subIndex" }
                            }
                            ?: error("Missing the requested token ${transactionPayload.address.run { "$index, $subIndex" }}")

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
                    "\ntoken=$token"
        )

        val method = getTransactionMethodName(transactionPayload)
        val accountAtDisposalBalance = account.balanceAtDisposal
        val sponsorAddress = partiallySignedTransaction.header.sponsor.get().toString()

        val reviewState = when (val transactionPayload = this.transactionPayload) {
            is AccountTransactionPayload.Transfer ->
                State.SessionRequestReview.TransactionRequestReview(
                    method = method,
                    receiver = transactionPayload.toAddress,
                    amount = transactionPayload.amount,
                    token = token,
                    estimatedFee = BigInteger.ZERO,
                    account = account,
                    canShowDetails = false,
                    isEnoughFunds = transactionPayload.amount <= accountAtDisposalBalance,
                    sponsor = sponsorAddress,
                    appMetadata = appMetadata,
                )

            is AccountTransactionPayload.Update ->
                State.SessionRequestReview.TransactionRequestReview(
                    method = method,
                    receiver = transactionPayload.address.run { "$index, $subIndex" },
                    amount = transactionPayload.amount,
                    token = token,
                    estimatedFee = BigInteger.ZERO,
                    account = account,
                    canShowDetails = true,
                    isEnoughFunds = transactionPayload.amount <= accountAtDisposalBalance,
                    sponsor = sponsorAddress,
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
                    estimatedFee = BigInteger.ZERO,
                    account = account,
                    canShowDetails = transactionPayload.transfer.memo.isPresent,
                    isEnoughFunds = tokenAmount <= token.balance,
                    sponsor = sponsorAddress,
                    appMetadata = appMetadata,
                )
            }
        }

        emitState(reviewState)
    }

    suspend fun onAuthorizedForApproval(
        accountKeys: AccountData,
    ) {
        setIsSubmittingTransaction(true)

        try {
            val submissionId = submitTransaction(
                signerEntry = accountKeys.getSignerEntry(),
            )

            respondSuccess(App.appCore.gson.toJson(TransactionSuccess(submissionId)))

            emitState(
                State.TransactionSubmitted(
                    submissionId = submissionId,
                    estimatedFee = BigInteger.ZERO,
                    isSponsored = true,
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


    private suspend fun submitTransaction(
        signerEntry: SignerEntry,
    ): String {
        val transaction =
            TransactionFactory
                .completeSponsoredTransaction(partiallySignedTransaction)
                .asSender(TransactionSigner.from(signerEntry))

        return proxyRepository
            .submitSdkTransaction(transaction)
            .submissionId
    }

    suspend fun onShowDetailsClicked() {

        when (val transactionPayload = this.transactionPayload) {
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

            is AccountTransactionPayload.Transfer
                -> {
                Log.w("Nothing to show as details for ${transactionPayload::class.simpleName}")

                emitEvent(
                    Event.ShowDetailsDialog(
                        title = getTransactionMethodName(transactionPayload),
                        prettyPrintDetails = context.getString(R.string.wallet_connect_transaction_request_no_details),
                    )
                )
            }

            is AccountTransactionPayload.Update -> {
                val transactionParamSchema =
                    AccountTransactionParams
                        .fromSessionRequestParams(transactionPayload.message)
                        .schema

                val prettyPrintParams = if (transactionParamSchema != null)
                    App.appCore.cryptoLibrary
                        .parameterToJson(
                            ParameterToJsonInput(
                                parameter = transactionPayload.message,
                                receiveName = transactionPayload.receiveName,
                                schema = transactionParamSchema,
                                schemaVersion = transactionParamSchema.version,
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
                        prettyPrintDetails = prettyPrintParams
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
                -> context.getString(R.string.transaction_type_transfer)

            is AccountTransactionPayload.Update ->
                transactionPayload.receiveName
        }

    companion object {
        const val METHOD = "sign_and_send_sponsored_transaction"
    }
}
