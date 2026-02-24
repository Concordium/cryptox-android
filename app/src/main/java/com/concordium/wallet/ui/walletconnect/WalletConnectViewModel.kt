@file:Suppress("ConvertObjectToDataObject")

package com.concordium.wallet.ui.walletconnect

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.extension.collect
import com.concordium.wallet.ui.walletconnect.delegate.LoggingWalletConnectCoreDelegate
import com.concordium.wallet.ui.walletconnect.delegate.LoggingWalletConnectWalletDelegate
import com.concordium.wallet.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
 * - [WalletConnectSignTransactionRequestHandler.METHOD] – create, sign and submit an account transaction
 * of the requested type. Send back the submission ID;
 * - [WalletConnectSignMessageRequestHandler.METHOD] – sign the given text message. Send back the signature;
 * - [WalletConnectVerifiablePresentationRequestHandler.METHOD] - prove or reveal identity statements;
 * - [WalletConnectVerifiablePresentationV1RequestHandler.METHOD] - prove or reveal identity statements via auditable proofs.
 *
 * @see State
 */
class WalletConnectViewModel
private constructor(
    application: Application,
    private val defaultCoreDelegate: CoreClient.CoreDelegate,
    private val defaultWalletDelegate: SignClient.WalletDelegate,
) : AndroidViewModel(application),
    CoreClient.CoreDelegate by defaultCoreDelegate,
    SignClient.WalletDelegate by defaultWalletDelegate,
    KoinComponent {

    /**
     * A constructor for Android.
     */
    @Suppress("unused")
    constructor(application: Application) : this(
        application = application,
        defaultCoreDelegate = LoggingWalletConnectCoreDelegate(),
        defaultWalletDelegate = LoggingWalletConnectWalletDelegate(),
    )

    private val allowedChains: Set<String> =
        BuildConfig.WC_CHAINS.toSet()
    private val wcUriPrefixes = setOf(
        WC_URI_PREFIX,
        "${application.getString(R.string.wc_scheme)}:",
    )
    private val allowedRequestMethods = setOf(
        WalletConnectSignTransactionRequestHandler.METHOD,
        WalletConnectSignMessageRequestHandler.METHOD,
        WalletConnectVerifiablePresentationRequestHandler.METHOD,
        WalletConnectVerifiablePresentationV1RequestHandler.METHOD,
        WalletConnectSignSponsoredTransactionRequestHandler.METHOD,
    )

    private val accountRepository: AccountRepository by lazy {
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    }
    private val proxyRepository: ProxyRepository by lazy(::ProxyRepository)
    private val identityRepository: IdentityRepository by lazy {
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    }
    private val tokensInteractor: TokensInteractor by inject()

    private lateinit var sessionProposalPublicKey: String
    private lateinit var sessionProposalNamespaceKey: String
    private lateinit var sessionProposalNamespace: Sign.Model.Namespace.Proposal

    private var sessionRequestId: Long = 0L
    private lateinit var sessionRequestTopic: String
    private lateinit var sessionRequestAccount: Account
    private lateinit var sessionRequestAppMetadata: AppMetadata
    private val handledRequests = mutableSetOf<Long>()

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

    private val isBusyFlow = MutableStateFlow(false)
    val isSessionRequestApproveButtonEnabledFlow: Flow<Boolean> =
        isBusyFlow.combine(stateFlow) { isBusy, state ->
            !isBusy && state is State.SessionRequestReview && state.canApprove
        }
    private var goBack = false

    private val signTransactionRequestHandler: WalletConnectSignTransactionRequestHandler by lazy {
        WalletConnectSignTransactionRequestHandler(
            respondSuccess = ::respondSuccess,
            respondError = ::respondError,
            emitEvent = mutableEventsFlow::tryEmit,
            emitState = mutableStateFlow::tryEmit,
            onFinish = ::onSessionRequestHandlingFinished,
            setIsSubmittingTransaction = isBusyFlow::tryEmit,
            proxyRepository = proxyRepository,
            tokensInteractor = tokensInteractor,
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

    private val verifiablePresentationV1RequestHandler: WalletConnectVerifiablePresentationV1RequestHandler by lazy {
        WalletConnectVerifiablePresentationV1RequestHandler(
            respondSuccess = ::respondSuccess,
            respondError = ::respondError,
            emitEvent = mutableEventsFlow::tryEmit,
            emitState = mutableStateFlow::tryEmit,
            onFinish = ::onSessionRequestHandlingFinished,
            setIsLoading = isBusyFlow::tryEmit,
            proxyRepository = proxyRepository,
            identityRepository = identityRepository,
            walletSetupPreferences = App.appCore.session.walletStorage.setupPreferences,
            activeWalletType = App.appCore.session.activeWallet.type,
        )
    }

    private val signSponsoredTransactionRequestHandler: WalletConnectSignSponsoredTransactionRequestHandler by lazy {
        WalletConnectSignSponsoredTransactionRequestHandler(
            respondSuccess = ::respondSuccess,
            respondError = ::respondError,
            emitEvent = mutableEventsFlow::tryEmit,
            emitState = mutableStateFlow::tryEmit,
            onFinish = ::onSessionRequestHandlingFinished,
            setIsSubmittingTransaction = isBusyFlow::tryEmit,
            proxyRepository = proxyRepository,
            tokensInteractor = tokensInteractor,
            context = application,
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
     * @param originalUri either 'wc' or app specific WalletConnect URI.
     * For example:
     * - cryptox-wc://wc?uri=wc%3A00e46b69
     * - wc:00e46b69...
     * - cryptox-wc://r?requestId=1759846918165693
     * - wc:00e46b69@2/wc?requestId=1759846918165693
     */
    fun handleUri(originalUri: String) {
        if (wcUriPrefixes.none { originalUri.startsWith(it) }) {
            Log.w(
                "uri_scheme_invalid:" +
                        "\nuri=$originalUri," +
                        "\nsupported:${wcUriPrefixes.joinToString(",")}"
            )

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidLink
                )
            )

            return
        }

        val actualUri: String =
            originalUri.toUri()
                .takeIf(Uri::isHierarchical)
                ?.getQueryParameter("uri")
                ?.also {
                    Log.d(
                        "using_uri_from_query:" +
                                "\noriginalUri=$originalUri," +
                                "\nactualUri=$it"
                    )
                }
                ?: originalUri

        // Request URI contains a corresponding query param.
        val isRequestUri = actualUri.contains("$WC_URI_REQUEST_ID_PARAM=")

        // Pairing URI is always 'wc'.
        val isPairingUri = !isRequestUri && actualUri.startsWith(WC_URI_PREFIX)

        goBack = originalUri.contains(WC_GO_BACK_PARAM) || actualUri.contains(WC_GO_BACK_PARAM)

        Log.d(
            "handling_uri:" +
                    "\nuri=$actualUri," +
                    "\nisRequestUri=$isRequestUri," +
                    "\nisPairingUri=$isPairingUri," +
                    "\ngoBack=$goBack"
        )

        when {
            isRequestUri ->
                onRequestWcUriReceived()

            isPairingUri ->
                pair(originalUri)

            else -> {
                Log.w(
                    "cant_handle_uri:" +
                            "\nuri=$actualUri"
                )

                mutableEventsFlow.tryEmit(
                    Event.ShowFloatingError(
                        Error.InvalidLink
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

    override fun onSessionProposal(
        sessionProposal: Sign.Model.SessionProposal,
        verifyContext: Sign.Model.VerifyContext,
    ) = viewModelScope.launch {
        defaultWalletDelegate.onSessionProposal(sessionProposal, verifyContext)

        // Find a single acceptable namespace.
        val singleNamespaceEntry =
            (sessionProposal.requiredNamespaces.entries + sessionProposal.optionalNamespaces.entries)
                .find { (_, namespace) ->
                    allowedChains.containsAll(namespace.chains.orEmpty())
                }

        val proposerPublicKey = sessionProposal.proposerPublicKey

        if (singleNamespaceEntry == null) {
            Log.e("cant_find_supported_chains")
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.NoSupportedChains
                )
            )
            rejectSession(
                proposerPublicKey,
                "The proposal did not contain a namespace where all chains are supported. " +
                        "Supported chains are: $allowedChains"
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

        // Initially select the current active account.
        // Fall back to the first one if the active one is not yet ready.
        val initiallySelectedAccount =
            accounts
                .firstOrNull(Account::isActive)
                ?: accounts.first()

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
        accountRepository
            .getAllDone()
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
                        chains = sessionProposalNamespace.chains,
                        accounts = sessionProposalNamespace.chains!!.map { chain ->
                            "$chain:$accountAddress"
                        },
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
        handleGoBack()
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
        handleGoBack()
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
                    accounts = accounts,
                    appMetadata = reviewState.appMetadata,
                    previousState = reviewState,
                )
            )
        }
    }

    fun onAccountSelected(selectedAccount: Account) {
        val selectionState = checkNotNull(state as? State.AccountSelection) {
            "The account can only be selected in the account selection state"
        }

        when (val previousState = selectionState.previousState) {
            is State.SessionProposalReview ->
                mutableStateFlow.tryEmit(
                    State.SessionProposalReview(
                        selectedAccount = selectedAccount,
                        appMetadata = selectionState.appMetadata
                    )
                )

            is State.SessionRequestReview.IdentityProofRequestReview ->
                if (previousState.isV1) {
                    verifiablePresentationV1RequestHandler.onAccountSelected(
                        claimIndex = previousState.currentClaim,
                        account = selectedAccount,
                    )
                } else {
                    verifiablePresentationRequestHandler.onAccountSelected(
                        statementIndex = previousState.currentClaim,
                        account = selectedAccount,
                    )
                }

            else ->
                error("Nothing to do with the selected account")
        }
    }

    fun onIdentitySelected(selectedIdentity: Identity) {
        val selectionState = checkNotNull(state as? State.IdentitySelection) {
            "The identity can only be selected in the identity selection state"
        }

        when (val previousState = selectionState.previousState) {
            is State.SessionRequestReview.IdentityProofRequestReview ->
                if (previousState.isV1) {
                    verifiablePresentationV1RequestHandler.onIdentitySelected(
                        claimIndex = previousState.currentClaim,
                        identity = selectedIdentity,
                    )
                } else {
                    error("Identity can't be changed for V0 proof request")
                }

            else ->
                error("Nothing to do with the selected identity")
        }
    }

    fun onChangeIdentityProofAccountClicked(
        statementIndex: Int,
    ) = viewModelScope.launch {
        val proofReviewState = state
        check(proofReviewState is State.SessionRequestReview.IdentityProofRequestReview) {
            "Choose account button can only be clicked in the proof request review state"
        }

        if (proofReviewState.isV1) {
            verifiablePresentationV1RequestHandler.onChangeAccountClicked(
                claimIndex = statementIndex,
                availableAccounts = getAvailableAccounts(),
            )
        } else {
            verifiablePresentationRequestHandler.onChangeAccountClicked(
                statementIndex = statementIndex,
                availableAccounts = getAvailableAccounts(),
            )
        }
    }

    fun onChangeIdentityProofIdentityClicked(
        statementIndex: Int,
    ) = viewModelScope.launch {
        val proofReviewState = state
        check(
            proofReviewState is State.SessionRequestReview.IdentityProofRequestReview
                    && proofReviewState.isV1
        ) {
            "Choose identity button can only be clicked in the proof request V1 review state"
        }

        verifiablePresentationV1RequestHandler.onChangeIdentityClicked(
            claimIndex = statementIndex,
        )
    }

    override fun onSessionRequest(
        sessionRequest: Sign.Model.SessionRequest,
        verifyContext: Sign.Model.VerifyContext,
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

        handleSessionRequest(
            topic = sessionRequest.topic,
            id = sessionRequest.request.id,
            method = sessionRequest.request.method,
            params = sessionRequest.request.params,
            peerMetadata = sessionRequestPeerMetadata
        )
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
                WalletConnectSignTransactionRequestHandler.METHOD ->
                    signTransactionRequestHandler.start(
                        params = params,
                        account = sessionRequestAccount,
                        appMetadata = sessionRequestAppMetadata,
                    )

                WalletConnectSignMessageRequestHandler.METHOD ->
                    signMessageRequestHandler.start(
                        params = params,
                        account = sessionRequestAccount,
                        appMetadata = sessionRequestAppMetadata,
                    )

                WalletConnectVerifiablePresentationRequestHandler.METHOD -> {
                    verifiablePresentationRequestHandler.start(
                        params = params,
                        account = account,
                        availableAccounts = getAvailableAccounts(),
                        appMetadata = sessionRequestAppMetadata,
                    )
                }

                WalletConnectVerifiablePresentationV1RequestHandler.METHOD -> {
                    verifiablePresentationV1RequestHandler.start(
                        params = params,
                        connectedAccount = account,
                        availableAccounts = getAvailableAccounts(),
                        appMetadata = sessionRequestAppMetadata,
                    )
                }

                WalletConnectSignSponsoredTransactionRequestHandler.METHOD -> {
                    signSponsoredTransactionRequestHandler.start(
                        params = params,
                        account = account,
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
        isBusyFlow.tryEmit(false)
        if (!handleGoBack()) {
            handleNextOldestPendingSessionRequest()
        }
    }

    private fun handleNextOldestPendingSessionRequest() {
        val activeSessions = SignClient.getListOfActiveSessions()
        val pendingRequestWithMetaData: Pair<Sign.Model.SessionRequest, Core.Model.AppMetaData?>? =
            activeSessions
                .map { session ->
                    SignClient
                        .getPendingSessionRequests(session.topic)
                        .filter { pendingRequest ->
                            // Do not include requests just handled.
                            pendingRequest.request.id !in handledRequests
                        }
                        .map { pendingRequest ->
                            pendingRequest to session.metaData
                        }
                }
                .flatten()
                .minByOrNull { (pendingRequest, _) ->
                    pendingRequest.request.id
                }

        val pendingRequest: Sign.Model.SessionRequest? = pendingRequestWithMetaData?.first
        if (pendingRequest == null) {
            Log.d("no_pending_requests_left")
            return
        }

        val peerMetaData: Core.Model.AppMetaData? = pendingRequestWithMetaData.second
        if (peerMetaData == null) {
            Log.d(
                "pending_request_has_no_metadata:" +
                        "\nrequestId=${pendingRequest.request.id}"
            )
            return
        }

        Log.d(
            "handling_pending_request:" +
                    "\nrequestId=${pendingRequest.request.id}"
        )

        handleSessionRequest(
            topic = pendingRequest.topic,
            id = pendingRequest.request.id,
            method = pendingRequest.request.method,
            params = pendingRequest.request.params,
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
                    if (reviewState.isSponsored) {
                        signSponsoredTransactionRequestHandler.onAuthorizedForApproval(accountKeys)
                    } else {
                        signTransactionRequestHandler.onAuthorizedForApproval(accountKeys)
                    }

                is State.SessionRequestReview.IdentityProofRequestReview -> {
                    if (reviewState.isV1) {
                        verifiablePresentationV1RequestHandler.onAuthorizedForApproval(password)
                    } else {
                        verifiablePresentationRequestHandler.onAuthorizedForApproval(password)
                    }
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

    fun getIdentityFromRepository(account: Account) = runBlocking(Dispatchers.IO) {
        identityRepository.getAllDone().firstOrNull { it.id == account.identityId }
    }

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

        if (reviewState.isSponsored) {
            signSponsoredTransactionRequestHandler.onShowDetailsClicked()
        } else {
            signTransactionRequestHandler.onShowDetailsClicked()
        }
    }

    fun onDialogCancelled() {
        when (val state = state) {
            State.Idle -> {
                // Do nothing.
            }

            State.WaitingForSessionProposal,
            State.WaitingForSessionRequest,
                -> {
                mutableStateFlow.tryEmit(State.Idle)
            }

            is State.AccountSelection -> {
                if (state.previousState is State.SessionRequestReview) {
                    rejectSessionRequest()
                } else {
                    rejectSessionProposal()
                }
            }

            is State.IdentitySelection -> {
                if (state.previousState is State.SessionRequestReview) {
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
                if (state.isSponsored) {
                    signSponsoredTransactionRequestHandler.onTransactionSubmittedViewClosed()
                } else {
                    signTransactionRequestHandler.onTransactionSubmittedViewClosed()
                }
            }
        }
        handleGoBack()
    }

    fun onTransactionSubmittedFinishClicked() {
        if ((state as State.TransactionSubmitted).isSponsored) {
            signSponsoredTransactionRequestHandler.onTransactionSubmittedFinishClicked()
        } else {
            signTransactionRequestHandler.onTransactionSubmittedFinishClicked()
        }
    }

    /**
     * @return true if the system back pressed handling must be intercepted.
     */
    fun onBackPressed(): Boolean =
        when (val state = this.state) {
            is State.AccountSelection -> {
                Log.d(
                    "switching_back_from_account_selection"
                )

                mutableStateFlow.tryEmit(state.previousState)

                true
            }

            else ->
                false
        }

    private fun respondSuccess(result: String) = viewModelScope.launch {
        handledRequests += sessionRequestId

        SignClient.respond(
            Sign.Params.Response(
                sessionTopic = sessionRequestTopic,
                jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcResult(
                    id = sessionRequestId,
                    result = result,
                )
            ),
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

    private fun handleGoBack(): Boolean {
        return if (goBack) {
            mutableEventsFlow.tryEmit(Event.GoBack)
            goBack = false
            true
        } else {
            false
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
            iconUrl = metadata.icons.firstOrNull(),
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
    | +------------> SessionRequestReview <-----------------+      IdentitySelection
    |                        | ^                                           ^
    |                        | +-------------------------------------------+
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
            val accounts: List<Account>,
            val appMetadata: AppMetadata,
            val previousState: State,
        ) : State

        /**
         * The user must select an identity among the available
         * in order to continue.
         */
        class IdentitySelection(
            val identities: List<Identity>,
            val previousState: State,
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
                val receiver: String,
                val amount: BigInteger,
                val token: Token,
                val estimatedFee: BigInteger,
                val canShowDetails: Boolean,
                val isEnoughFunds: Boolean,
                val sponsor: String?,
                account: Account,
                appMetadata: AppMetadata,
            ) : SessionRequestReview(
                account = account,
                appMetadata = appMetadata,
                canApprove = isEnoughFunds,
            ) {
                val isSponsored: Boolean =
                    sponsor != null
            }

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
                val claims: List<IdentityProofRequestClaims>,
                val currentClaim: Int,
                val provable: ProofProvableState,
                val isV1: Boolean,
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
            val isSponsored: Boolean,
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
         * The dApp opened the wallet with an invalid URI.
         */
        object InvalidLink : Error

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

        /**
         * Event to navigate back after handling the URI
         */
        object GoBack : Event
    }

    private companion object {
        private const val WC_URI_PREFIX = "wc:"
        private const val WC_URI_REQUEST_ID_PARAM = "requestId"
        private const val DEFAULT_ERROR_RESPONSE_CODE = 500
        private const val WC_GO_BACK_PARAM = "go_back=true"
    }
}
