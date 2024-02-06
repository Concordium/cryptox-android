package com.concordium.wallet.ui.walletconnect

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.SignMessageInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.model.TransferCost
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.walletconnect.Params
import com.concordium.wallet.data.walletconnect.Payload
import com.concordium.wallet.data.walletconnect.TransactionSuccess
import com.concordium.wallet.extension.collect
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.REQUEST_METHOD_SIGN_MESSAGE
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.State
import com.concordium.wallet.ui.walletconnect.delegate.LoggingWalletConnectCoreDelegate
import com.concordium.wallet.ui.walletconnect.delegate.LoggingWalletConnectWalletDelegate
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger
import com.google.gson.Gson
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A view model that handles WalletConnect pairing, session proposals and requests.
 *
 * *Pairing* – opening a transport connection with a dApp;
 *
 * *Session proposal* – a proposal from a dApp to establish a session within an active pairing
 * to perform actions with a particular account;
 *
 * *Session request* – a request from a dApp within an active session that requires performing
 * some action with the account of the session.
 *
 * Currently supported requests:
 * - [REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION] – create, sign and submit an account transaction
 * of the requested type. Send back the submission ID;
 * - [REQUEST_METHOD_SIGN_MESSAGE] – sign the given text message. Send back the signature.
 *
 * @see State
 * @see allowedRequestMethods
 * @see allowedAccountTransactionTypeMap
 */
class WalletConnectViewModel
private constructor(
    application: Application,
    private val defaultCoreDelegate: CoreClient.CoreDelegate,
    private val defaultWalletDelegate: SignClient.WalletDelegate,
) : AndroidViewModel(application),
    CoreClient.CoreDelegate by defaultCoreDelegate,
    SignClient.WalletDelegate by defaultWalletDelegate {

    /**
     * A constructor for Android.
     */
    @Suppress("unused")
    constructor(application: Application) : this(
        application = application,
        defaultCoreDelegate = LoggingWalletConnectCoreDelegate(),
        defaultWalletDelegate = LoggingWalletConnectWalletDelegate(),
    )

    @Suppress("KotlinConstantConditions")
    private val allowedChains: Set<String> =
        if (BuildConfig.ENV_NAME == "production")
            setOf("ccd:mainnet")
        else
            setOf("ccd:testnet")
    private val wcUriPrefixes = setOf(
        WC_URI_PREFIX,
        "${application.getString(R.string.wc_scheme)}:",
    )
    private val allowedAccountTransactionTypeMap = mapOf(
        TransactionType.UPDATE to TransactionTypeValues(
            cryptoLibraryType = "Update",
            transactionCostType = ProxyRepository.UPDATE,
        ),
        TransactionType.TRANSFER to TransactionTypeValues(
            cryptoLibraryType = "Transfer",
            transactionCostType = ProxyRepository.SIMPLE_TRANSFER,
        ),
    )
    private val allowedRequestMethods = setOf(
        REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION,
        REQUEST_METHOD_SIGN_MESSAGE,
    )

    private val accountRepository: AccountRepository by lazy {
        AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
    }
    private val proxyRepository: ProxyRepository by lazy(::ProxyRepository)

    private lateinit var sessionProposalPublicKey: String
    private lateinit var sessionProposalNamespaceKey: String
    private lateinit var sessionProposalNamespaceChain: String
    private lateinit var sessionProposalNamespace: Sign.Model.Namespace.Proposal

    private var sessionRequestId: Long = 0L
    private lateinit var sessionRequestTopic: String
    private lateinit var sessionRequestParams: Params
    private lateinit var sessionRequestParamsPayload: Payload
    private lateinit var sessionRequestAccount: Account
    private lateinit var sessionRequestAppMetadata: AppMetadata
    private lateinit var sessionRequestTransactionCost: BigInteger
    private lateinit var sessionRequestTransactionNonce: AccountNonce
    private lateinit var sessionRequestMessageToSign: String

    private val handledRequests = mutableSetOf<Long>()

    private val mutableStateFlow = MutableStateFlow<State>(State.Idle)
    val stateFlow: Flow<State> = mutableStateFlow
    val state: State
        get() = mutableStateFlow.value

    private val mutableEventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = mutableEventsFlow

    private val mutableIsSubmittingTransactionFlow = MutableStateFlow(false)
    private val isBusyFlow: Flow<Boolean> = mutableIsSubmittingTransactionFlow
    val isSessionRequestApproveButtonEnabledFlow: Flow<Boolean> =
        isBusyFlow.combine(stateFlow) { isBusy, state ->
            !isBusy && state is State.SessionRequestReview && state.canApprove
        }

    init {
        stateFlow.collect(viewModelScope) { state ->
            Log.d(
                "state_changed:" +
                        "\nnewState=$state"
            )
        }

        eventsFlow.collect(viewModelScope) { event ->
            Log.d(
                "event_emitted:" +
                        "\nevent=$event"
            )
        }
    }

    fun initialize() {
        CoreClient.setDelegate(this)
        SignClient.setWalletDelegate(this)

        Log.d(
            "initialized:" +
                    "\npairingsCount=${CoreClient.Pairing.getPairings().size}," +
                    "\nsettledSessionsCount=${SignClient.getListOfSettledSessions().size}"
        )

        handleNextOldestPendingSessionRequest()
    }

    /**
     * @param wcUri 'wc:' pair or redirect request URI
     *
     * @see R.string.wc_scheme
     */
    fun handleWcUri(wcUri: String) {
        if (wcUriPrefixes.none { wcUri.startsWith(it) }) {
            Log.w(
                "url_scheme_invalid:" +
                        "\nuri=$wcUri," +
                        "\nsupported:${wcUriPrefixes.joinToString(",")}"
            )

            return
        }

        // Request URI contains a corresponding query param,
        // but may have 'wc' or redirect scheme.
        val isRequestUri = wcUri.contains("$WC_URI_REQUEST_ID_PARAM=")

        // Pairing URI is always 'wc'.
        val isPairingUri = !isRequestUri && wcUri.startsWith(WC_URI_PREFIX)

        Log.d(
            "handling_uri:" +
                    "\nuri=$wcUri," +
                    "\nisRequestUri=$isRequestUri," +
                    "\nisPairingUri=$isPairingUri"
        )

        when {
            isRequestUri ->
                onRequestWcUriReceived()

            isPairingUri ->
                pair(wcUri)

            else -> {
                Log.w(
                    "cant_handle_uri:" +
                            "\nuri=$wcUri"
                )

                mutableEventsFlow.tryEmit(
                    Event.ShowFloatingError(
                        Error.InvalidRequest
                    )
                )
            }
        }
    }

    private fun pair(wcUri: String) {
        val pairingParams = Core.Params.Pair(wcUri)

        Log.d(
            "pairing:" +
                    "\nparams=$pairingParams"
        )

        val currentPairings = CoreClient.Pairing.getPairings()

        // Disconnect all the other pairings.
        currentPairings.forEach { currentPairing ->
            Log.d(
                "disconnect_active_pairing:" +
                        "\npairing=$currentPairing"
            )

            CoreClient.Pairing.disconnect(Core.Params.Disconnect(currentPairing.topic)) { error ->
                Log.e(
                    "failed_active_pairing_disconnect:" +
                            "\npairing=$currentPairing", error.throwable
                )
            }
        }

        Log.d("making_new_pairing")

        CoreClient.Pairing.pair(pairingParams) { error ->
            Log.e("failed_pairing", error.throwable)

            // If the pairing is failed,
            // end WaitingForSessionProposal and show the error.
            mutableStateFlow.tryEmit(State.Idle)
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.ConnectionFailed
                )
            )
        }

        mutableStateFlow.tryEmit(State.WaitingForSessionProposal)
    }

    private fun onRequestWcUriReceived() {
        // When receiving the request URI, enter the request waiting state.
        // The request will arrive soon over the websocket.
        mutableStateFlow.tryEmit(State.WaitingForSessionRequest)
    }

    override fun onSessionProposal(
        sessionProposal: Sign.Model.SessionProposal,
    ) = viewModelScope.launch {
        defaultWalletDelegate.onSessionProposal(sessionProposal)

        // Find a single allowed namespace and chain.
        val singleNamespaceEntry =
            sessionProposal.requiredNamespaces.entries.find { (_, namespace) ->
                namespace.chains.any { chain ->
                    allowedChains.contains(chain)
                }
            }
        val singleNamespaceChain = singleNamespaceEntry?.value?.chains?.find { chain ->
            allowedChains.contains(chain)
        }

        if (singleNamespaceEntry == null || singleNamespaceChain == null) {
            Log.e("cant_find_supported_chain")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.NoSupportedChains
                )
            )
            mutableStateFlow.tryEmit(State.Idle)
            return@launch
        }

        Log.d("loading_accounts")

        val accounts = getAvailableAccounts()
        if (accounts.isEmpty()) {
            Log.d("there_are_no_accounts")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.NoAccounts
                )
            )
            mutableStateFlow.tryEmit(State.Idle)
            return@launch
        }

        this@WalletConnectViewModel.sessionProposalPublicKey = sessionProposal.proposerPublicKey
        this@WalletConnectViewModel.sessionProposalNamespaceKey = singleNamespaceEntry.key
        this@WalletConnectViewModel.sessionProposalNamespace = singleNamespaceEntry.value
        this@WalletConnectViewModel.sessionProposalNamespaceChain = singleNamespaceChain

        // Initially select the account with the biggest balance.
        val initiallySelectedAccount = accounts.maxBy { account ->
            account.getAtDisposalWithoutStakedOrScheduled(account.totalUnshieldedBalance)
        }

        Log.d(
            "handling_session_proposal:" +
                    "\ninitiallySelectedAccountId=${initiallySelectedAccount.id}"
        )

        mutableStateFlow.tryEmit(
            State.SessionProposalReview(
                selectedAccount = initiallySelectedAccount,
                appMetadata = AppMetadata(sessionProposal),
            )
        )
    }.let { /* Return nothing */ }

    private suspend fun getAvailableAccounts(): List<Account> =
        accountRepository.getAllDone()
            .filterNot(Account::readOnly)

    fun approveSessionProposal() {
        val sessionProposalReviewState = checkNotNull(state as? State.SessionProposalReview) {
            "Session proposal approval is only possible in the proposal review state"
        }

        val accountAddress = sessionProposalReviewState.selectedAccount.address

        SignClient.approveSession(
            Sign.Params.Approve(
                proposerPublicKey = sessionProposalPublicKey,
                namespaces = mapOf(
                    sessionProposalNamespaceKey to Sign.Model.Namespace.Session(
                        accounts = listOf("$sessionProposalNamespaceChain:$accountAddress"),
                        methods = sessionProposalNamespace.methods,
                        events = sessionProposalNamespace.events,
                        extensions = null,
                    )
                ),
            )
        ) { error ->
            Log.e("failed_approving_session", error.throwable)

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.ResponseFailed
                )
            )
        }

        mutableStateFlow.tryEmit(State.Idle)
    }

    fun rejectSessionProposal() {
        check(state is State.SessionProposalReview || state is State.AccountSelection) {
            "Session proposal rejection is only possible in the proposal review " +
                    "or account selection states"
        }

        SignClient.rejectSession(
            Sign.Params.Reject(
                proposerPublicKey = sessionProposalPublicKey,
                reason = "Rejected by user",
            )
        ) { error ->
            Log.e("failed_rejecting_session", error.throwable)

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.ResponseFailed
                )
            )
        }

        mutableStateFlow.tryEmit(State.Idle)
    }

    fun onChooseAccountClicked() {
        val reviewState = checkNotNull(state as? State.SessionProposalReview) {
            "Choose account button can only be clicked in the proposal review state"
        }

        viewModelScope.launch {
            val accounts = getAvailableAccounts()

            Log.d(
                "switching_to_account_selection:" +
                        "\navailableAccounts=${accounts.size}"
            )

            mutableStateFlow.emit(
                State.AccountSelection(
                    selectedAccount = reviewState.selectedAccount,
                    accounts = accounts,
                    appMetadata = reviewState.appMetadata,
                )
            )
        }
    }

    fun onAccountSelected(selectedAccount: Account) {
        val selectionState = checkNotNull(state as? State.AccountSelection) {
            "The account can only be selected in the account selection state"
        }

        Log.d(
            "switching_to_session_proposal_review:" +
                    "\nnewSelectedAccountId=${selectedAccount.id}"
        )

        mutableStateFlow.tryEmit(
            State.SessionProposalReview(
                selectedAccount = selectedAccount,
                appMetadata = selectionState.appMetadata,
            )
        )
    }

    override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest) {
        defaultWalletDelegate.onSessionRequest(sessionRequest)

        val sessionRequestPeerMetadata: Core.Model.AppMetaData? = sessionRequest.peerMetaData
        if (sessionRequestPeerMetadata == null) {
            Log.e("missing_app_metadata_in_session_request")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )
            return
        }

        if (state !is State.SessionRequestReview) {
            // Only handle the request if not reviewing another one.
            // Otherwise, the request will be handled as a pending one
            // once the current is reviewed.
            handleSessionRequest(
                topic = sessionRequest.topic,
                id = sessionRequest.request.id,
                method = sessionRequest.request.method,
                params = sessionRequest.request.params,
                peerMetadata = sessionRequestPeerMetadata
            )
        } else {
            Log.d(
                "session_request_to_be_handled_later:" +
                        "\nrequestId=${sessionRequest.request.id}"
            )
        }
    }

    private fun handleSessionRequest(
        topic: String,
        id: Long,
        method: String,
        params: String,
        peerMetadata: Core.Model.AppMetaData,
    ) = viewModelScope.launch {
        // Needs to be set first to allow sending responses.
        this@WalletConnectViewModel.sessionRequestId = id
        this@WalletConnectViewModel.sessionRequestTopic = topic

        val sessionRequestParams: Params = try {
            check(method in allowedRequestMethods) {
                "Request method '$method' is not supported"
            }

            Params.fromSessionRequestParams(params).also { parsedParams ->
                check(allowedAccountTransactionTypeMap.containsKey(parsedParams.type)) {
                    "Transaction type '${parsedParams.type}' is not supported"
                }
            }
        } catch (error: Exception) {
            Log.e("failed_parsing_session_request_params", error)

            respondError(
                message = "Failed parse the request: $error"
            )

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )
            return@launch
        }

        val sessionRequestParamsPayload: Payload? = sessionRequestParams.parsePayload()
        if (sessionRequestParamsPayload == null) {
            Log.e("failed_parsing_session_request_params_payload")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )
            return@launch
        }

        val sessionRequestAccount: Account? =
            accountRepository.findByAddress(sessionRequestParams.sender)
        if (sessionRequestAccount == null) {
            Log.e("missing_account_required_for_session_request")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.MissingRequestedAccount
                )
            )
            return@launch
        }

        this@WalletConnectViewModel.sessionRequestParams = sessionRequestParams
        this@WalletConnectViewModel.sessionRequestParamsPayload = sessionRequestParamsPayload
        this@WalletConnectViewModel.sessionRequestAccount = sessionRequestAccount
        this@WalletConnectViewModel.sessionRequestAppMetadata = peerMetadata
            .let(WalletConnectViewModel::AppMetadata)

        Log.d(
            "handling_session_request:" +
                    "\nparams=$sessionRequestParams"
        )

        when (method) {
            REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION ->
                onSignAndSendTransactionRequested()

            REQUEST_METHOD_SIGN_MESSAGE ->
                onSignMessageRequested()
        }
    }

    private fun handleNextOldestPendingSessionRequest() {
        val settledSessions = SignClient.getListOfSettledSessions()
        val pendingRequestWithMetaData: Pair<Sign.Model.PendingRequest, Core.Model.AppMetaData?>? =
            settledSessions
                .map { session ->
                    SignClient.getPendingRequests(session.topic)
                        .filter { pendingRequest ->
                            // Do not include requests just handled.
                            pendingRequest.requestId !in handledRequests
                        }
                        .map { pendingRequest ->
                            pendingRequest to session.metaData
                        }
                }
                .flatten()
                .minByOrNull { (pendingRequest, _) ->
                    pendingRequest.requestId
                }

        val pendingRequest: Sign.Model.PendingRequest? = pendingRequestWithMetaData?.first
        if (pendingRequest == null) {
            Log.d("no_pending_requests_left")
            return
        }

        val peerMetaData: Core.Model.AppMetaData? = pendingRequestWithMetaData.second
        if (peerMetaData == null) {
            Log.d(
                "pending_request_has_no_metadata:" +
                        "\nrequestId=${pendingRequest.requestId}"
            )
            return
        }

        Log.d(
            "handling_pending_request:" +
                    "\nrequestId=${pendingRequest.requestId}"
        )

        handleSessionRequest(
            topic = pendingRequest.topic,
            id = pendingRequest.requestId,
            method = pendingRequest.method,
            params = pendingRequest.params,
            peerMetadata = peerMetaData,
        )
    }

    private fun onSignAndSendTransactionRequested() {
        loadTransactionRequestDataAndReview()
    }

    private fun loadTransactionRequestDataAndReview() = viewModelScope.launch {
        val accountNonce: AccountNonce
        val transactionCostResponse: TransferCost

        Log.d("loading_data")

        try {
            accountNonce = try {
                getSessionRequestAccountNonce()
            } catch (error: Exception) {
                Log.e("failed_loading_transaction_nonce", error)
                throw error
            }

            transactionCostResponse = try {
                getSessionRequestTransactionCost()
            } catch (error: Exception) {
                Log.e("failed_loading_transaction_cost", error)
                throw error
            }
        } catch (error: Exception) {
            respondError(
                message = "Failed loading transaction data: $error"
            )

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.LoadingFailed
                )
            )

            return@launch
        }

        Log.d(
            "data_loaded:" +
                    "\ncost=$transactionCostResponse," +
                    "\nnonce=$accountNonce"
        )

        val transactionCost = transactionCostResponse.cost.toBigInteger()
        val accountAtDisposalBalance =
            sessionRequestAccount.getAtDisposalWithoutStakedOrScheduled(
                sessionRequestAccount.totalUnshieldedBalance
            )
        val isEnoughFunds =
            accountAtDisposalBalance >= (sessionRequestParamsPayload.amount.toBigInteger() + transactionCost)

        sessionRequestTransactionCost = transactionCost
        sessionRequestParamsPayload.maxEnergy = transactionCostResponse.energy
        sessionRequestTransactionNonce = accountNonce

        mutableStateFlow.tryEmit(
            State.SessionRequestReview.TransactionRequestReview(
                method = sessionRequestParamsPayload.receiveName,
                estimatedFee = sessionRequestTransactionCost,
                account = sessionRequestAccount,
                isEnoughFunds = isEnoughFunds,
                appMetadata = sessionRequestAppMetadata,
            )
        )
    }

    private suspend fun getSessionRequestTransactionCost()
            : TransferCost = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getTransferCost(
            type = checkNotNull(allowedAccountTransactionTypeMap[sessionRequestParams.type]) {
                "The unsupported transaction type should have been rejected before"
            }.transactionCostType,
            amount = sessionRequestParamsPayload.amount.toBigInteger(),
            sender = sessionRequestAccount.address,
            contractIndex = sessionRequestParamsPayload.address.index,
            contractSubindex = sessionRequestParamsPayload.address.subIndex,
            receiveName = sessionRequestParamsPayload.receiveName,
            parameter = sessionRequestParamsPayload.message,
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private suspend fun getSessionRequestAccountNonce()
            : AccountNonce = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getAccountNonce(
            accountAddress = sessionRequestAccount.address,
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    fun rejectSessionRequest() {
        Log.d("rejecting_session_request")

        respondError("Rejected by user")

        mutableStateFlow.tryEmit(State.Idle)
        handleNextOldestPendingSessionRequest()
    }

    fun approveSessionRequest() {
        val reviewState = checkNotNull(state as? State.SessionRequestReview) {
            "Approval is only possible in a session request review state"
        }

        check(reviewState.canApprove) {
            "Trying to approve a request that can't be approved"
        }

        Log.d("approving_session_request")

        // Continue in onAuthenticated
        mutableEventsFlow.tryEmit(Event.ShowAuthentication)
    }

    fun onAuthenticated(password: String) {
        when (state) {
            is State.SessionRequestReview -> {
                decryptAccountAndApproveRequest(password)
            }

            else -> {
                Log.w("ignore_outdated_authentication")
            }
        }
    }

    private fun decryptAccountAndApproveRequest(password: String) = viewModelScope.launch {
        val reviewState = checkNotNull(state as? State.SessionRequestReview) {
            "Approval is only possible in a session request review state"
        }

        val storageAccountDataEncrypted = sessionRequestAccount.encryptedAccountData
        if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
            Log.e("account_has_no_encrypted_data")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.CryptographyFailed
                )
            )
            return@launch
        }
        val decryptedJson = App.appCore.getCurrentAuthenticationManager()
            .decryptInBackground(password, storageAccountDataEncrypted)
        if (decryptedJson != null) {
            val accountKeys =
                App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
                    .accountKeys

            when (reviewState) {
                is State.SessionRequestReview.SignRequestReview ->
                    signRequestedMessage(accountKeys)

                is State.SessionRequestReview.TransactionRequestReview ->
                    signAndSubmitRequestedTransaction(accountKeys)
            }
        } else {
            Log.e("failed_account_decryption")
            respondError(
                message = "Failed account decryption: crypto library internal error"
            )

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.CryptographyFailed
                )
            )
        }
    }

    private fun onSignMessageRequested() {
        val messageToSign: String? = sessionRequestParams.message
        if (messageToSign == null) {
            Log.e("missing_message_to_sign")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )
            return
        }

        this.sessionRequestMessageToSign = messageToSign

        mutableStateFlow.tryEmit(
            State.SessionRequestReview.SignRequestReview(
                message = messageToSign,
                account = sessionRequestAccount,
                appMetadata = sessionRequestAppMetadata
            )
        )
    }

    private suspend fun signRequestedMessage(accountKeys: AccountData) {
        val signMessageInput = SignMessageInput(
            address = sessionRequestAccount.address,
            message = sessionRequestMessageToSign,
            keys = accountKeys,
        )
        val signMessageOutput = App.appCore.cryptoLibrary.signMessage(signMessageInput)
        if (signMessageOutput == null) {
            Log.e("failed_signing_message")
            respondError(
                message = "Failed signing message: crypto library internal error"
            )

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.CryptographyFailed
                )
            )
        } else {
            respondSuccess(
                result = Gson().toJson(signMessageOutput)
            )

            mutableStateFlow.tryEmit(State.Idle)
            handleNextOldestPendingSessionRequest()
        }
    }

    private suspend fun signAndSubmitRequestedTransaction(accountKeys: AccountData) {
        val accountTransactionInput = CreateAccountTransactionInput(
            expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000,
            from = sessionRequestAccount.address,
            keys = accountKeys,
            nonce = sessionRequestTransactionNonce.nonce,
            payload = sessionRequestParamsPayload,
            type = checkNotNull(allowedAccountTransactionTypeMap[sessionRequestParams.type]) {
                "The unsupported transaction type should have been rejected before"
            }.cryptoLibraryType,
        )

        val accountTransactionOutput =
            App.appCore.cryptoLibrary.createAccountTransaction(accountTransactionInput)
        if (accountTransactionOutput == null) {
            Log.e("failed_creating_transaction")
            respondError(
                message = "Failed creating transaction: crypto library internal error"
            )

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.CryptographyFailed
                )
            )
        } else {
            val createTransferOutput = CreateTransferOutput(
                transaction = accountTransactionOutput.transaction,
                signatures = accountTransactionOutput.signatures,
                addedSelfEncryptedAmount = null,
                remaining = null,
            )
            submitTransaction(createTransferOutput)
        }
    }

    private var submitTransactionRequest: BackendRequest<*>? = null
    private fun submitTransaction(createTransferOutput: CreateTransferOutput) {
        mutableIsSubmittingTransactionFlow.tryEmit(true)

        submitTransactionRequest?.dispose()
        submitTransactionRequest = proxyRepository.submitTransfer(createTransferOutput,
            { submissionData ->
                Log.d(
                    "transaction_submitted:" +
                            "\nsubmissionId=${submissionData.submissionId}"
                )
                respondSuccess(
                    result = Gson().toJson(TransactionSuccess(submissionData.submissionId))
                )

                mutableIsSubmittingTransactionFlow.tryEmit(false)
                mutableStateFlow.tryEmit(
                    State.TransactionSubmitted(
                        submissionId = submissionData.submissionId,
                        estimatedFee = sessionRequestTransactionCost,
                    )
                )
            },
            { error ->
                Log.e("failed_submitting_transaction", error)
                respondError(
                    message = "Failed submitting transaction: $error"
                )

                mutableIsSubmittingTransactionFlow.tryEmit(false)
                mutableEventsFlow.tryEmit(
                    Event.ShowFloatingError(
                        Error.TransactionSubmitFailed
                    )
                )
            }
        )
    }

    fun onDialogCancelled() {
        when (state) {
            State.Idle -> {
                // Do nothing.
            }

            State.WaitingForSessionProposal,
            State.WaitingForSessionRequest -> {
                mutableStateFlow.tryEmit(State.Idle)
            }

            is State.AccountSelection,
            is State.SessionProposalReview -> {
                rejectSessionProposal()
            }

            is State.SessionRequestReview -> {
                rejectSessionRequest()
            }

            is State.TransactionSubmitted -> {
                onTransactionSubmittedViewClosed()
            }
        }
    }

    fun onTransactionSubmittedFinishClicked() {
        onTransactionSubmittedViewClosed()
    }

    /**
     * @return true if the system back pressed handling must be intercepted.
     */
    fun onBackPressed(): Boolean =
        when (val state = this.state) {
            is State.AccountSelection -> {
                Log.d(
                    "switching_back_to_session_proposal_review"
                )

                mutableStateFlow.tryEmit(
                    State.SessionProposalReview(
                        selectedAccount = state.selectedAccount,
                        appMetadata = state.appMetadata,
                    )
                )
                true
            }

            else ->
                false
        }

    private fun onTransactionSubmittedViewClosed() {
        mutableStateFlow.tryEmit(State.Idle)
        handleNextOldestPendingSessionRequest()
    }

    private fun respondSuccess(result: String) {
        handledRequests += sessionRequestId

        SignClient.respond(
            Sign.Params.Response(
                sessionTopic = sessionRequestTopic,
                jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcResult(
                    id = sessionRequestId,
                    result = result,
                )
            )
        ) { error ->
            Log.e("failed_responding_success", error.throwable)

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.ResponseFailed
                )
            )
        }
    }

    private fun respondError(message: String) {
        handledRequests += sessionRequestId

        SignClient.respond(
            Sign.Params.Response(
                sessionTopic = sessionRequestTopic,
                jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcError(
                    id = sessionRequestId,
                    code = DEFAULT_ERROR_RESPONSE_CODE,
                    message = message,
                )
            )
        ) { error ->
            Log.e("failed_responding_error", error.throwable)

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.ResponseFailed
                )
            )
        }
    }

    override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
        defaultWalletDelegate.onSessionDelete(deletedSession)
        mutableStateFlow.tryEmit(State.Idle)
    }

    override fun onCleared() {
        CoreClient.setDelegate(defaultCoreDelegate)
        SignClient.setWalletDelegate(defaultWalletDelegate)
    }

    data class AppMetadata(
        val name: String,
        val iconUrl: String?,
        val url: String,
    ) {
        constructor(proposal: Sign.Model.SessionProposal) : this(
            name = proposal.name,
            iconUrl = proposal.icons.firstOrNull()?.toString(),
            url = proposal.url,
        )

        constructor(metadata: Core.Model.AppMetaData) : this(
            name = metadata.name,
            iconUrl = metadata.icons.firstOrNull()?.toString(),
            url = metadata.url,
        )
    }

    /**
    ```
    Idle   <----> WaitingForSessionProposal
    ^ ^ ^ ^                  |
    | | | |                  |
    | | | |                  v
    | | | +-------> SessionProposalReview   <---->  AccountSelection
    | | |
    | | |
    | | |
    | | +--------> WaitingForSessionRequest
    | |                      |
    | |                      |
    | |                      v
    | +------------> SessionRequestReview
    |                        |
    |                        |
    |                        v
    +--------------- TransactionSubmitted
    ```
    States containing accounts should not be data classes to avoid printout.
     */
    sealed interface State {
        /**
         * No actions are required from the user.
         * Proposals and requests may come.
         */
        object Idle : State

        /**
         * Explicitly waiting for a session proposal after pairing.
         * No actions are required from the user.
         */
        object WaitingForSessionProposal : State

        /**
         * The user must review a session proposal.
         */
        class SessionProposalReview(
            val selectedAccount: Account,
            val appMetadata: AppMetadata,
        ) : State

        /**
         * The user must select an account among the available
         * in order to continue.
         */
        class AccountSelection(
            val selectedAccount: Account,
            val accounts: List<Account>,
            val appMetadata: AppMetadata,
        ) : State

        /**
         * Explicitly waiting for a session request after receiving a request URI.
         * No actions are required from the user.
         */
        object WaitingForSessionRequest : State

        /**
         * The user must review a session request.
         */
        sealed class SessionRequestReview(
            val account: Account,
            val appMetadata: AppMetadata,
            val canApprove: Boolean,
        ) : State {
            class TransactionRequestReview(
                val method: String,
                val estimatedFee: BigInteger,
                val isEnoughFunds: Boolean,
                account: Account,
                appMetadata: AppMetadata,
            ) : SessionRequestReview(
                account = account,
                appMetadata = appMetadata,
                canApprove = isEnoughFunds,
            )

            class SignRequestReview(
                val message: String,
                account: Account,
                appMetadata: AppMetadata,
            ) : SessionRequestReview(
                account = account,
                appMetadata = appMetadata,
                canApprove = true,
            )
        }

        /**
         * Showing the submitted transaction summary
         * after [SessionRequestReview.TransactionRequestReview].
         * No actions are required from the user.
         */
        class TransactionSubmitted(
            val submissionId: String,
            val estimatedFee: BigInteger,
        ) : State
    }

    sealed interface Error {
        /**
         * Transport connection (pairing) with the dApp failed.
         */
        object ConnectionFailed : Error

        /**
         * The dApp sent a session proposal without any supported chains.
         */
        object NoSupportedChains : Error

        /**
         * The dApp sent a request that can't be parsed.
         */
        object InvalidRequest : Error

        /**
         * The dApp sent a request for an account not in the wallet.
         */
        object MissingRequestedAccount : Error

        /**
         * There are no accounts in the wallet therefore a session can't be established.
         */
        object NoAccounts : Error

        /**
         * Some data required to prepare a request review is failed to load.
         */
        object LoadingFailed : Error

        /**
         * Failed to send a response to the dApp.
         */
        object ResponseFailed : Error

        /**
         * The transaction for [State.SessionRequestReview.TransactionRequestReview]
         * is failed to submit.
         */
        object TransactionSubmitFailed : Error

        /**
         * There is a problem decrypting the account or the crypto library failed.
         */
        object CryptographyFailed : Error
    }

    sealed interface Event {
        /**
         * An authentication must be performed to continue.
         * @see onAuthenticated
         */
        object ShowAuthentication : Event

        /**
         * The user must be notified about the occurred [error].
         */
        class ShowFloatingError(
            val error: Error,
        ) : Event
    }

    private data class TransactionTypeValues(
        val cryptoLibraryType: String,
        val transactionCostType: String,
    )

    private companion object {
        private const val WC_URI_PREFIX = "wc:"
        private const val WC_URI_REQUEST_ID_PARAM = "requestId"

        private const val DEFAULT_ERROR_RESPONSE_CODE = 500

        private const val REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION = "sign_and_send_transaction"
        private const val REQUEST_METHOD_SIGN_MESSAGE = "sign_message"
    }
}
