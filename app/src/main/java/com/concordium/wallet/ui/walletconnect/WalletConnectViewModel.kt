@file:Suppress("ConvertObjectToDataObject")

package com.concordium.wallet.ui.walletconnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.sdk.crypto.wallet.web3Id.UnqualifiedRequest
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.extension.collect
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.REQUEST_METHOD_SIGN_MESSAGE
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Companion.REQUEST_METHOD_VERIFIABLE_PRESENTATION
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.State
import com.concordium.wallet.ui.walletconnect.delegate.LoggingWalletConnectCoreDelegate
import com.concordium.wallet.ui.walletconnect.delegate.LoggingWalletConnectWalletDelegate
import com.concordium.wallet.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigInteger

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
 * - [REQUEST_METHOD_SIGN_MESSAGE] – sign the given text message. Send back the signature;
 * - [REQUEST_METHOD_VERIFIABLE_PRESENTATION] - prove or reveal identity statements.
 *
 * @see State
 * @see allowedAccountTransactionTypes
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
    private val allowedAccountTransactionTypes = setOf(
        TransactionType.UPDATE,
        TransactionType.TRANSFER,
    )
    private val allowedRequestMethods = setOf(
        REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION,
        REQUEST_METHOD_SIGN_MESSAGE,
        REQUEST_METHOD_VERIFIABLE_PRESENTATION,
    )

    private val accountRepository: AccountRepository by lazy {
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    }
    private val proxyRepository: ProxyRepository by lazy(::ProxyRepository)
    private val identityRepository: IdentityRepository by lazy {
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    }

    private lateinit var sessionProposalPublicKey: String
    private lateinit var sessionProposalNamespaceKey: String
    private lateinit var sessionProposalNamespaceChain: String
    private lateinit var sessionProposalNamespace: Sign.Model.Namespace.Proposal

    private var sessionRequestId: Long = 0L
    private lateinit var sessionRequestTopic: String
    private lateinit var sessionRequestAccount: Account
    private lateinit var sessionRequestAppMetadata: AppMetadata
    private val handledRequests = mutableSetOf<Long>()
    private val connectionAvailabilityFlow = MutableStateFlow(false)

    enum class ProofProvableState {
        Provable,
        UnProvable,
        NoCompatibleIssuer
    }

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

    private val signTransactionRequestHandler: WalletConnectSignTransactionRequestHandler by lazy {
        WalletConnectSignTransactionRequestHandler(
            allowedAccountTransactionTypes = allowedAccountTransactionTypes,
            respondSuccess = ::respondSuccess,
            respondError = ::respondError,
            emitEvent = mutableEventsFlow::tryEmit,
            emitState = mutableStateFlow::tryEmit,
            onFinish = ::onSessionRequestHandlingFinished,
            setIsSubmittingTransaction = mutableIsSubmittingTransactionFlow::tryEmit,
            proxyRepository = proxyRepository,
            context = application,
        )
    }

    private val signMessageRequestHandler: WalletConnectSignMessageRequestHandler by lazy {
        WalletConnectSignMessageRequestHandler(
            respondSuccess = ::respondSuccess,
            respondError = ::respondError,
            emitEvent = mutableEventsFlow::tryEmit,
            emitState = mutableStateFlow::tryEmit,
            onFinish = ::onSessionRequestHandlingFinished,
            context = application,
        )
    }

    private val verifiablePresentationRequestHandler: WalletConnectVerifiablePresentationRequestHandler by lazy {
        WalletConnectVerifiablePresentationRequestHandler(
            respondSuccess = ::respondSuccess,
            respondError = ::respondError,
            emitEvent = mutableEventsFlow::tryEmit,
            emitState = mutableStateFlow::tryEmit,
            onFinish = ::onSessionRequestHandlingFinished,
            proxyRepository = proxyRepository,
            identityRepository = identityRepository,
            activeWalletType = App.appCore.session.activeWallet.type,
        )
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
                    "\nactiveSessionsCount=${SignClient.getListOfActiveSessions().size}"
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

            @Suppress("DEPRECATION")
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
        if (state !is State.SessionRequestReview) {
            mutableStateFlow.tryEmit(State.WaitingForSessionRequest)
        }
    }

    override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
        defaultWalletDelegate.onConnectionStateChange(state)
        connectionAvailabilityFlow.tryEmit(state.isAvailable)
    }

    override fun onSessionProposal(
        sessionProposal: Sign.Model.SessionProposal,
        verifyContext: Sign.Model.VerifyContext
    ) = viewModelScope.launch {
        defaultWalletDelegate.onSessionProposal(sessionProposal, verifyContext)

        // Find a single allowed namespace and chain.
        val singleNamespaceEntry =
            sessionProposal.requiredNamespaces.entries.find { (_, namespace) ->
                namespace.chains?.any { chain ->
                    allowedChains.contains(chain)
                } == true
            }
        val singleNamespaceChain = singleNamespaceEntry?.value?.chains?.find { chain ->
            allowedChains.contains(chain)
        }

        val proposerPublicKey = sessionProposal.proposerPublicKey

        if (singleNamespaceEntry == null || singleNamespaceChain == null) {
            Log.e("cant_find_supported_chain")
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.NoSupportedChains
                )
            )
            rejectSession(
                proposerPublicKey,
                "The session proposal did not contain a valid namespace. Allowed namespaces are: $allowedChains"
            )
            return@launch
        }

            // Check if the proposer requests unsupported methods, and reject the session proposal
            // if that is the case.
            val requestedMethods = singleNamespaceEntry.value.methods
            if (!allowedRequestMethods.containsAll(requestedMethods)) {
                Log.e("Received an unsupported request method: $requestedMethods")
                mutableEventsFlow.tryEmit(
                    Event.ShowFloatingError(
                        Error.UnsupportedMethod
                    )
                )
                rejectSession(
                    proposerPublicKey,
                    "An unsupported method was requested: $requestedMethods, supported methods are $allowedRequestMethods"
                )
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
                rejectSession(
                    proposerPublicKey,
                    "The wallet does not contain any accounts to open a session for"
                )
                return@launch
            }

            this@WalletConnectViewModel.sessionProposalPublicKey = proposerPublicKey
            this@WalletConnectViewModel.sessionProposalNamespaceKey = singleNamespaceEntry.key
            this@WalletConnectViewModel.sessionProposalNamespace = singleNamespaceEntry.value
            this@WalletConnectViewModel.sessionProposalNamespaceChain = singleNamespaceChain

            // Initially select the account with the biggest balance.
            val initiallySelectedAccount = accounts.maxBy { account ->
                account.balanceAtDisposal
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
                        chains = listOf(sessionProposalNamespaceChain),
                        accounts = listOf("$sessionProposalNamespaceChain:$accountAddress"),
                        methods = sessionProposalNamespace.methods,
                        events = sessionProposalNamespace.events,
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

    private fun rejectSession(proposerPublicKey: String, reason: String) {
        val rejectParams = Sign.Params.Reject(
            proposerPublicKey = proposerPublicKey,
            reason = reason
        )

        SignClient.rejectSession(rejectParams) { error ->
            Log.e("failed_rejecting_session", error.throwable)
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
        rejectSession(sessionProposalPublicKey, "Rejected by user")
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
                    identityProofPosition = null
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

        if (!selectionState.forIdentityProof) {
            mutableStateFlow.tryEmit(
                State.SessionProposalReview(
                    selectedAccount = selectedAccount,
                    appMetadata = selectionState.appMetadata
                )
            )
        } else {
            verifiablePresentationRequestHandler.onAccountSelected(
                statementIndex = selectionState.identityProofPosition!!,
                account = selectedAccount,
            )
        }
    }

    fun onChangeIdentityProofAccountClicked(
        statementIndex: Int,
    ) = viewModelScope.launch {
        check(state is State.SessionRequestReview.IdentityProofRequestReview) {
            "Choose account button can only be clicked in the proof request review state"
        }

        verifiablePresentationRequestHandler.onChangeAccountClicked(
            statementIndex = statementIndex,
            availableAccounts = getAvailableAccounts(),
        )
    }

    override fun onSessionRequest(
        sessionRequest: Sign.Model.SessionRequest,
        verifyContext: Sign.Model.VerifyContext
    ) {
        defaultWalletDelegate.onSessionRequest(sessionRequest, verifyContext)
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

        // We do not want to queue more than one request for each topic (dApp),
        // as the dApp may be broken and spam requests.
        // At this point, topic pending requests may already contain this sessionRequest.
        val topicPendingRequests = SignClient.getPendingSessionRequests(sessionRequest.topic)
        if (topicPendingRequests.isEmpty() || topicPendingRequests.size == 1) {
            // Only handle the request if the session ID is different from the current one
            // Otherwise, ignore the re-processing call
            if (sessionRequest.request.id != this.sessionRequestId) {
                handleSessionRequest(
                    topic = sessionRequest.topic,
                    id = sessionRequest.request.id,
                    method = sessionRequest.request.method,
                    params = sessionRequest.request.params,
                    peerMetadata = sessionRequestPeerMetadata
                )
            } else {
                Log.w(
                    "received_next_session_request_in_topic_with_the_same_id" +
                    "\ndon't handle the request again"
                )
            }
        } else {
            Log.w(
                "received_next_session_request_in_topic_before_current_is_handled:" +
                        "\nrequestId=${topicPendingRequests.last().request.id}" +
                        "\nrequestTopic=${topicPendingRequests.last().topic}"
            )

            respondError(
                message = "Subsequent requests are rejected until the current one is handled: " +
                        topicPendingRequests.first().request.id,
                sessionRequestId = topicPendingRequests.last().request.id,
                sessionRequestTopic = topicPendingRequests.last().topic,
            )
        }
    }

    /**
     * Extracts the account address from a map of session namespaces. This works as the account
     * format follows the convention: `ccd:network:accountAddress`, which is defined by what we set
     * in the namespace in [approveSessionProposal].
     * If the account address cannot be found, then a null value is returned instead.
     */
    private fun extractAccountAddressFromNamespaces(namespaces: Map<String, Sign.Model.Namespace.Session>): String? {
        val concordiumNamespaceSession = namespaces["ccd"]
        if (concordiumNamespaceSession == null) {
            Log.e("Namespaces did not contain the ccd namespace")
            return null
        }

        // A WalletConnect session should always be for exactly one account. If there are more, then
        // we cannot uniquely determine the correct account address.
        if (concordiumNamespaceSession.accounts.size != 1) {
            Log.e("There was not exactly one account in the namespace session")
            return null
        }

        val accountLine = concordiumNamespaceSession.accounts[0]
        accountLine.split(":").let {
            if (it.size != 3) {
                Log.e("The account address line did not split into the expected chunks: $accountLine")
                return null
            }
            return it[2]
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
        this@WalletConnectViewModel.sessionRequestAppMetadata = peerMetadata
            .let(WalletConnectViewModel::AppMetadata)

        // Find the session that the request matches. The session will allow us to extract
        // the account that the request is for.
        val currentSession = SignClient.getActiveSessionByTopic(topic)
        if (currentSession == null) {
            Log.e("Received a request for a topic where we did not find the session: Topic=$topic")

            respondError(message = "No session found for the received topic: $topic")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )

            onSessionRequestHandlingFinished()

            return@launch
        }

        // Extract the account address from the namespaces. This is expected to always work
        // unless the creation of the session is altered.
        val accountAddress = extractAccountAddressFromNamespaces(currentSession.namespaces)
        if (accountAddress == null) {
            Log.e("Unable to extract account address from the session namespaces")

            respondError(message = "Broken session")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )

            onSessionRequestHandlingFinished()

            return@launch
        }

        // Load the account from the repository. This is expected to always find an account
        // as the account address in the session has been provided by the wallet.
        val account = accountRepository.findByAddress(accountAddress)
        if (account == null) {
            Log.e("Unable to find account with address: $accountAddress")

            respondError(message = "Unable to find the namespace account")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.AccountNotFound
                )
            )

            onSessionRequestHandlingFinished()

            return@launch
        }

        this@WalletConnectViewModel.sessionRequestAccount = account

        if (method !in allowedRequestMethods) {
            val unsupportedMethodMessage =
                "Received an unsupported WalletConnect method request: $method"

            Log.e(unsupportedMethodMessage)

            respondError(message = unsupportedMethodMessage)

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )

            onSessionRequestHandlingFinished()

            return@launch
        }

        // Even though handlers contain specific error handling,
        // the global try-catch prevents the crash loop
        // if there is an unexpected error.
        try {
            when (method) {
                REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION ->
                    signTransactionRequestHandler.start(
                        params = params,
                        account = sessionRequestAccount,
                        appMetadata = sessionRequestAppMetadata,
                    )

                REQUEST_METHOD_SIGN_MESSAGE ->
                    signMessageRequestHandler.start(
                        params = params,
                        account = sessionRequestAccount,
                        appMetadata = sessionRequestAppMetadata,
                    )

                REQUEST_METHOD_VERIFIABLE_PRESENTATION -> {
                    verifiablePresentationRequestHandler.start(
                        params = params,
                        account = account,
                        availableAccounts = getAvailableAccounts(),
                        appMetadata = sessionRequestAppMetadata,
                    )
                }

                else ->
                    error("Missing a handler for the allowed method '$method'")
            }
        } catch (error: Exception) {
            val unexpectedErrorMessage = "Unexpected error occurred: $error"

            Log.e(unexpectedErrorMessage, error)

            respondError(unexpectedErrorMessage)

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )

            onSessionRequestHandlingFinished()
        }
    }

    private fun onSessionRequestHandlingFinished() {
        mutableStateFlow.tryEmit(State.Idle)
        handleNextOldestPendingSessionRequest()
    }

    private fun handleNextOldestPendingSessionRequest() {
        val activeSessions = SignClient.getListOfActiveSessions()
        val pendingRequestWithMetaData: Pair<Sign.Model.PendingRequest, Core.Model.AppMetaData?>? =
            activeSessions
                .map { session ->
                    SignClient.getPendingSessionRequests(session.topic)
                        .filterIsInstance<Sign.Model.PendingRequest>() // Ensure the type matches
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

    fun rejectSessionRequest() {
        Log.d("rejecting_session_request")

        respondError("Rejected by user")

        onSessionRequestHandlingFinished()
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
        if (storageAccountDataEncrypted == null) {
            Log.e("account_has_no_encrypted_data")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.CryptographyFailed
                )
            )

            return@launch
        }
        val decryptedJson = App.appCore.auth
            .decrypt(
                password = password,
                encryptedData = storageAccountDataEncrypted,
            )
            ?.let(::String)

        if (decryptedJson != null) {
            val storageAccountData =
                App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
            val accountKeys = storageAccountData.accountKeys

            when (reviewState) {
                is State.SessionRequestReview.SignRequestReview ->
                    signMessageRequestHandler.onAuthorizedForApproval(accountKeys)

                is State.SessionRequestReview.TransactionRequestReview ->
                    signTransactionRequestHandler.onAuthorizedForApproval(accountKeys)

                is State.SessionRequestReview.IdentityProofRequestReview -> {
                    verifiablePresentationRequestHandler.onAuthorizedForApproval(password)
                }
            }
        } else {
            Log.e("failed_account_decryption")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.CryptographyFailed
                )
            )
        }
    }

    fun getIdentity(account: Account): Identity? =
        verifiablePresentationRequestHandler.getIdentity(account)

    fun onShowSignRequestDetailsClicked() = viewModelScope.launch {
        val reviewState = state as? State.SessionRequestReview.SignRequestReview
        check(reviewState != null && reviewState.canShowDetails) {
            "Show details button can only be clicked in the sign request review state " +
                    "allowing showing the details"
        }

        signMessageRequestHandler.showReviewingMessageDetails()
    }

    fun onShowTransactionRequestDetailsClicked() = viewModelScope.launch {
        val reviewState = state as? State.SessionRequestReview.TransactionRequestReview
        check(reviewState != null && reviewState.canShowDetails) {
            "Show details button can only be clicked in the transaction request review state " +
                    "allowing showing the details"
        }

        signTransactionRequestHandler.onShowDetailsClicked()
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

            is State.AccountSelection -> {
                if ((state as State.AccountSelection).forIdentityProof) {
                    rejectSessionRequest()
                } else {
                    rejectSessionProposal()
                }
            }

            is State.SessionProposalReview -> {
                rejectSessionProposal()
            }

            is State.SessionRequestReview -> {
                rejectSessionRequest()
            }

            is State.TransactionSubmitted -> {
                signTransactionRequestHandler.onTransactionSubmittedViewClosed()
            }
        }
    }

    fun onTransactionSubmittedFinishClicked() =
        signTransactionRequestHandler.onTransactionSubmittedFinishClicked()

    /**
     * @return true if the system back pressed handling must be intercepted.
     */
    fun onBackPressed(): Boolean =
        when (val state = this.state) {
            is State.AccountSelection -> {
                Log.d(
                    "switching_back_from_account_selection"
                )

                if (state.forIdentityProof) {
                    verifiablePresentationRequestHandler.onAccountSelectionBackPressed(
                        statementIndex = state.identityProofPosition!!,
                    )
                } else {
                    mutableStateFlow.tryEmit(
                        State.SessionProposalReview(
                            selectedAccount = state.selectedAccount,
                            appMetadata = state.appMetadata,
                        )
                    )
                }

                true
            }

            else ->
                false
        }

    private fun respondSuccess(result: String) = viewModelScope.launch {
        handledRequests += sessionRequestId

        // Await the connection.
        connectionAvailabilityFlow.first { it }

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

    private fun respondError(
        message: String,
        sessionRequestId: Long = this.sessionRequestId,
        sessionRequestTopic: String = this.sessionRequestTopic,
    ) = viewModelScope.launch {
        handledRequests += sessionRequestId

        // Await the connection.
        connectionAvailabilityFlow.first { it }

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
    | | |                                                   ^
    | | |                                                   |
    | | |                                                   |
    | | +--------> WaitingForSessionRequest                 |
    | |                      |                              |
    | |                      |                              |
    | |                      v                              |
    | +------------> SessionRequestReview <-----------------+
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
            val identityProofPosition: Int?,
            val appMetadata: AppMetadata,
        ) : State {
            val forIdentityProof = identityProofPosition != null
        }

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
                val receiver: String,
                val amount: BigInteger,
                val estimatedFee: BigInteger,
                val canShowDetails: Boolean,
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
                val canShowDetails: Boolean,
                account: Account,
                appMetadata: AppMetadata,
            ) : SessionRequestReview(
                account = account,
                appMetadata = appMetadata,
                canApprove = true,
            )

            class IdentityProofRequestReview(
                val request: UnqualifiedRequest,
                val chosenAccounts: List<Account>,
                val currentStatement: Int,
                val provable: ProofProvableState,
                connectedAccount: Account,
                appMetadata: AppMetadata,
            ) : SessionRequestReview(
                account = connectedAccount,
                appMetadata = appMetadata,
                // Check that we can prove the statement with current accounts
                canApprove = provable == ProofProvableState.Provable
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
         * The dApp sent a session proposal requesting an unsupported method.
         */
        object UnsupportedMethod : Error

        /**
         * The dApp sent a request that can't be parsed.
         */
        object InvalidRequest : Error

        /**
         * The dApp sent an account transaction request for a different account
         * than the one connected in the session.
         */
        object AccountNotFound : Error

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

        /**
         * An internal error occurred.
         */
        object InternalError : Error

        /**
         * The functionality is only supported by a seed phrase based wallet.
         */
        object NotSeedPhraseWalletError : Error
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

        /**
         * A dialog showing pretty print contract call details must be shown.
         * The details printout may be quite long.
         */
        class ShowDetailsDialog(
            val title: String?,
            val prettyPrintDetails: String,
        ) : Event
    }

    private companion object {
        private const val WC_URI_PREFIX = "wc:"
        private const val WC_URI_REQUEST_ID_PARAM = "requestId"

        private const val DEFAULT_ERROR_RESPONSE_CODE = 500

        private const val REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION = "sign_and_send_transaction"
        private const val REQUEST_METHOD_SIGN_MESSAGE = "sign_message"
        private const val REQUEST_METHOD_VERIFIABLE_PRESENTATION = "request_verifiable_presentation"
    }
}
