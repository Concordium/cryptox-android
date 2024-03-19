package com.concordium.wallet.ui.walletconnect

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.sdk.crypto.bulletproof.BulletproofGenerators
import com.concordium.sdk.crypto.pedersencommitment.PedersenCommitmentKey
import com.concordium.sdk.crypto.wallet.Network
import com.concordium.sdk.crypto.wallet.web3Id.AcceptableRequest
import com.concordium.sdk.crypto.wallet.web3Id.AccountCommitmentInput
import com.concordium.sdk.crypto.wallet.web3Id.Statement.IdentityQualifier
import com.concordium.sdk.crypto.wallet.web3Id.Statement.StatementType
import com.concordium.sdk.crypto.wallet.web3Id.Statement.UnqualifiedRequestStatement
import com.concordium.sdk.crypto.wallet.web3Id.UnqualifiedRequest
import com.concordium.sdk.crypto.wallet.web3Id.Web3IdProof
import com.concordium.sdk.crypto.wallet.web3Id.Web3IdProofInput
import com.concordium.sdk.responses.accountinfo.credential.AttributeType
import com.concordium.sdk.responses.cryptographicparameters.CryptographicParameters
import com.concordium.sdk.transactions.CredentialRegistrationId
import com.concordium.sdk.types.UInt32
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.AttributeRandomness
import com.concordium.wallet.data.cryptolib.CreateAccountTransactionInput
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.cryptolib.SignMessageInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.model.TransferCost
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.data.walletconnect.AccountTransactionParams
import com.concordium.wallet.data.walletconnect.AccountTransactionPayload
import com.concordium.wallet.data.walletconnect.SignMessageParams
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
        AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
    }
    private val proxyRepository: ProxyRepository by lazy(::ProxyRepository)

    private val identityRepository: IdentityRepository by lazy {
        IdentityRepository(WalletDatabase.getDatabase(getApplication()).identityDao())
    }

    private val authPreferences: AuthPreferences


    private lateinit var sessionProposalPublicKey: String
    private lateinit var sessionProposalNamespaceKey: String
    private lateinit var sessionProposalNamespaceChain: String
    private lateinit var sessionProposalNamespace: Sign.Model.Namespace.Proposal

    private var sessionRequestId: Long = 0L
    private lateinit var sessionRequestTopic: String
    private lateinit var sessionRequestAccountTransactionParams: AccountTransactionParams
    private lateinit var sessionRequestAccountTransactionPayload: AccountTransactionPayload
    private lateinit var sessionRequestAccount: Account
    private lateinit var sessionRequestAppMetadata: AppMetadata
    private lateinit var sessionRequestTransactionCost: BigInteger
    private lateinit var sessionRequestTransactionNonce: AccountNonce
    private lateinit var sessionRequestMessageToSign: String
    private lateinit var sessionRequestIdentityRequest: UnqualifiedRequest
    private lateinit var sessionRequestIdentityProofAccounts: MutableList<Account>
    private lateinit var sessionRequestIdentityProofIdentities: MutableMap<Int, Identity>
    private lateinit var sessionRequestIdentityProofProvable: ProofProvableState
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

        authPreferences = AuthPreferences(application)
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
            onAccountSelectedIdentityProof(selectedAccount, selectionState.identityProofPosition!!)
        }
    }

    fun onChooseAccountIdentityProof(position: Int) {
        val proofRequestReview =
            checkNotNull(state as? State.SessionRequestReview.IdentityProofRequestReview) {
                "Choose account button can only be clicked in the proof request review state"
            }

        viewModelScope.launch {
            val accounts = getAvailableAccounts()
            val statement = sessionRequestIdentityRequest.credentialStatements[position]

            Log.d(
                "switching_to_account_selection:" +
                        "\navailableAccounts=${accounts.size}"
            )

            val validAccounts = accounts.filter {
                val identity = getIdentity(it) ?: return@filter false
                isValidIdentityForStatement(identity, statement)
            }

            mutableStateFlow.emit(
                State.AccountSelection(
                    selectedAccount = proofRequestReview.chosenAccounts[position],
                    accounts = validAccounts.ifEmpty { accounts },
                    appMetadata = proofRequestReview.appMetadata,
                    identityProofPosition = position
                )
            )
        }
    }

    private fun createIdentityProofRequestState(currentStatement: Int): State.SessionRequestReview {
        return State.SessionRequestReview.IdentityProofRequestReview(
            connectedAccount = sessionRequestAccount,
            appMetadata = sessionRequestAppMetadata,
            request = sessionRequestIdentityRequest,
            chosenAccounts = sessionRequestIdentityProofAccounts,
            currentStatement = currentStatement,
            provable = sessionRequestIdentityProofProvable
        )
    }

    private fun onAccountSelectedIdentityProof(selectedAccount: Account, position: Int) {
        sessionRequestIdentityProofAccounts[position] = selectedAccount
        mutableStateFlow.tryEmit(
            createIdentityProofRequestState(position)
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

    private fun handleSignTransactionRequest(params: String) {
        val sessionRequestAccountTransactionParams: AccountTransactionParams = try {
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

            respondError(
                message = "Failed parse the request: $error"
            )

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )
            return
        }

        val sessionRequestAccountTransactionPayload: AccountTransactionPayload? =
            sessionRequestAccountTransactionParams.parsePayload()
        if (sessionRequestAccountTransactionPayload == null) {
            Log.e(
                "failed_parsing_session_request_params_payload:" +
                        "\npayload=${sessionRequestAccountTransactionParams.payload}"
            )

            respondError(
                message = "Failed parsing the request params payload"
            )

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )
            return
        }

        val senderAccountAddress = sessionRequestAccountTransactionParams.sender
        if (senderAccountAddress != sessionRequestAccount.address) {
            Log.e("Received a request to sign an account transaction for a different account than the one connected in the session: $senderAccountAddress")
            respondError(message = "The request was for a different account than the one connected.")
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.AccountMismatch
                )
            )
            return
        }

        this@WalletConnectViewModel.sessionRequestAccountTransactionParams =
            sessionRequestAccountTransactionParams
        this@WalletConnectViewModel.sessionRequestAccountTransactionPayload =
            sessionRequestAccountTransactionPayload

        Log.d(
            "handling_session_request:" +
                    "\nparams=$sessionRequestAccountTransactionParams"
        )

        onSignAndSendTransactionRequested()
    }

    private fun handleSignMessage(params: String) {
        try {
            val signMessageParams = SignMessageParams.fromSessionRequestParams(params)
            this@WalletConnectViewModel.sessionRequestMessageToSign = signMessageParams.message

            mutableStateFlow.tryEmit(
                State.SessionRequestReview.SignRequestReview(
                    message = sessionRequestMessageToSign,
                    account = sessionRequestAccount,
                    appMetadata = sessionRequestAppMetadata
                )
            )
        } catch (e: Exception) {
            Log.e("Failed to parse sign message parameters: $params", e)
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )
            return
        }
    }

    private fun onInvalidRequest(responseMessage: String, e: Exception? = null) {
        if (e == null) Log.e(responseMessage) else Log.e(responseMessage, e)
        mutableEventsFlow.tryEmit(
            Event.ShowFloatingError(
                Error.InvalidRequest
            )
        )
        respondError(responseMessage)
        mutableStateFlow.tryEmit(State.Idle)
    }

    private fun isValidIdentityForStatement(
        identity: Identity,
        statement: UnqualifiedRequestStatement
    ): Boolean {
        return statement.idQualifier is IdentityQualifier
                && (statement.idQualifier as IdentityQualifier).issuers.contains(identity.identityProviderId.toLong())
                && statement.canBeProvedBy(getIdentityObject(identity))
    }

    /// Initializes session variables for proof request session.
    /// Returns a boolean indicating whether initialization succeeded.
    private suspend fun initializeProofRequestSession(params: String): Boolean {
        if (!authPreferences.hasEncryptedSeed()) {
            Log.d("A non-seed phrase wallet does not support identity proofs")
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.NotSeedPhraseWalletError
                )
            )
            return false
        }
        try {
            val wrappedParams = Gson().fromJson(params, WalletConnectParamsWrapper::class.java)
            val identityProofRequest = UnqualifiedRequest.fromJson(wrappedParams.paramsJson)
            sessionRequestIdentityRequest = identityProofRequest
        } catch (e: Exception) {
            onInvalidRequest("Failed to parse identity proof request parameters: $params", e)
            return false
        }

        if (sessionRequestIdentityRequest.credentialStatements.any { it.statementType == StatementType.SmartContract }) {
            onInvalidRequest("Web3Id statements are not supported.")
            return false
        }

        try {
            // Check that the statement is acceptable. Using the session account here to qualify
            // is sufficient because we don't check that the statement is provable.
            val network =
                if (BuildConfig.ENV_NAME == "production") Network.MAINNET else Network.TESTNET
            val credId =
                CredentialRegistrationId.from(sessionRequestAccount.credential!!.getCredId()!!)
            AcceptableRequest.acceptableRequest(sessionRequestIdentityRequest.qualify { stat ->
                stat.qualify(
                    credId,
                    network
                )
            })
        } catch (e: Exception) {
            onInvalidRequest("Request received is not acceptable", e)
            return false
        }

        val identities = HashMap<Int, Identity>()
        identityRepository.getAllDone().forEach { identities[it.id] = it }
        sessionRequestIdentityProofIdentities = identities

        val accounts = getAvailableAccounts()
        val initialAccounts = sessionRequestIdentityRequest.credentialStatements.map { statement ->
            // Prefer session account
            if (isValidIdentityForStatement(identities[sessionRequestAccount.identityId]!!, statement))
                sessionRequestAccount
            else {
                // Otherwise find any account that can prove the statement
                accounts.find {
                    isValidIdentityForStatement(
                        identities[it.identityId]!!,
                        statement
                    )
                }
            }
        }

        sessionRequestIdentityProofAccounts = if (initialAccounts.any { it == null }) {
            // Check whether any statement does not have any account, which issuer is allowed.
            val noCompatibleIssuers =
                sessionRequestIdentityRequest.credentialStatements.any { statement ->
                    accounts.none { account ->
                        (statement.idQualifier as IdentityQualifier).issuers.contains(
                            identities[account.identityId]!!.identityProviderId.toLong()
                        )
                    }
                }
            sessionRequestIdentityProofProvable = if (noCompatibleIssuers) {
                ProofProvableState.NoCompatibleIssuer
            } else {
                ProofProvableState.UnProvable
            }

            initialAccounts.map { it ?: sessionRequestAccount }.toMutableList()
        } else {
            sessionRequestIdentityProofProvable = ProofProvableState.Provable
            initialAccounts.requireNoNulls().toMutableList()
        }
        return true
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
        val currentSession = SignClient.getSettledSessionByTopic(topic)
        if (currentSession == null) {
            Log.e("Received a request for a topic where we did not find the session: Topic=$topic")
            respondError(message = "No session found for the received topic: $topic")
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )
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
            return@launch
        }

        // Load the account from the repository. This is expected to always find an account
        // as the account address in the session has been provided by the wallet.
        val account = accountRepository.findByAddress(accountAddress)
        if (account == null) {
            Log.e("Unable to find account with address: $accountAddress")
            respondError(message = "Broken session")
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )
            return@launch
        }

        this@WalletConnectViewModel.sessionRequestAccount = account

        when (method) {
            REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION -> handleSignTransactionRequest(params)
            REQUEST_METHOD_SIGN_MESSAGE -> handleSignMessage(params)
            REQUEST_METHOD_VERIFIABLE_PRESENTATION -> {
                if (initializeProofRequestSession(params))
                    mutableStateFlow.tryEmit(
                        createIdentityProofRequestState(0)
                    )
            }

            else -> {
                val unsupportedMethodMessage =
                    "Received an unsupported WalletConnect method request: $method"
                Log.e(unsupportedMethodMessage)
                respondError(message = unsupportedMethodMessage)
                mutableEventsFlow.tryEmit(
                    Event.ShowFloatingError(
                        Error.InvalidRequest
                    )
                )
            }
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
        val accountTransactionPayload = sessionRequestAccountTransactionPayload
        sessionRequestTransactionCost = transactionCost
        sessionRequestTransactionNonce = accountNonce
        if (accountTransactionPayload is AccountTransactionPayload.Update) {
            accountTransactionPayload.maxEnergy = transactionCostResponse.energy
        }

        val reviewState = when (accountTransactionPayload) {
            is AccountTransactionPayload.Transfer ->
                State.SessionRequestReview.TransactionRequestReview(
                    method = getApplication<Application>().getString(R.string.transaction_type_transfer),
                    receiver = accountTransactionPayload.toAddress,
                    amount = accountTransactionPayload.amount,
                    estimatedFee = sessionRequestTransactionCost,
                    account = sessionRequestAccount,
                    isEnoughFunds = accountTransactionPayload.amount + transactionCost <= accountAtDisposalBalance,
                    appMetadata = sessionRequestAppMetadata,
                )

            is AccountTransactionPayload.Update ->
                State.SessionRequestReview.TransactionRequestReview(
                    method = accountTransactionPayload.receiveName,
                    receiver = accountTransactionPayload.address.run { "$index, $subIndex" },
                    amount = accountTransactionPayload.amount,
                    estimatedFee = sessionRequestTransactionCost,
                    account = sessionRequestAccount,
                    isEnoughFunds = accountTransactionPayload.amount + transactionCost <= accountAtDisposalBalance,
                    appMetadata = sessionRequestAppMetadata,
                )
        }
        mutableStateFlow.tryEmit(reviewState)
    }

    private suspend fun getSessionRequestTransactionCost(
    ): TransferCost = suspendCancellableCoroutine { continuation ->
        val backendRequest =
            when (val accountTransactionPayload = sessionRequestAccountTransactionPayload) {
                is AccountTransactionPayload.Transfer ->
                    proxyRepository.getTransferCost(
                        type = ProxyRepository.SIMPLE_TRANSFER,
                        success = continuation::resume,
                        failure = continuation::resumeWithException,
                    )

                is AccountTransactionPayload.Update ->
                    proxyRepository.getTransferCost(
                        type = ProxyRepository.UPDATE,
                        amount = accountTransactionPayload.amount,
                        sender = sessionRequestAccount.address,
                        contractIndex = accountTransactionPayload.address.index,
                        contractSubindex = accountTransactionPayload.address.subIndex,
                        receiveName = accountTransactionPayload.receiveName,
                        parameter = accountTransactionPayload.message,
                        success = continuation::resume,
                        failure = continuation::resumeWithException,
                    )
            }

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

    private suspend fun getAccountAttributes(
        account: Account,
        password: String
    ): AttributeRandomness? {
        val storageAccountDataEncrypted = account.encryptedAccountData
        if (TextUtils.isEmpty(storageAccountDataEncrypted)) {
            Log.e("account_has_no_encrypted_data")

            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.CryptographyFailed
                )
            )
            return null
        }
        val decryptedJson = App.appCore.getCurrentAuthenticationManager()
            .decryptInBackground(password, storageAccountDataEncrypted)
        val storageAccountData =
            App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
        return storageAccountData.getAttributeRandomness()
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
            val storageAccountData =
                App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
            val accountKeys = storageAccountData.accountKeys

            when (reviewState) {
                is State.SessionRequestReview.SignRequestReview ->
                    signRequestedMessage(accountKeys)

                is State.SessionRequestReview.TransactionRequestReview ->
                    signAndSubmitRequestedTransaction(accountKeys)

                is State.SessionRequestReview.IdentityProofRequestReview -> {
                    val attributeRandomness = HashMap<String, AttributeRandomness?>()
                    attributeRandomness[storageAccountData.accountAddress] =
                        storageAccountData.getAttributeRandomness()
                    reviewState.chosenAccounts.forEach {
                        if (!attributeRandomness.contains(it.address)) {
                            attributeRandomness[it.address] = getAccountAttributes(it, password)
                        }
                    }
                    createProofPresentation(reviewState.chosenAccounts, attributeRandomness)
                }
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

    private fun createProofPresentation(
        chosenAccounts: List<Account>,
        randomnessMap: Map<String, AttributeRandomness?>,
    ) {
        val request = sessionRequestIdentityRequest

        var accountIterator = chosenAccounts.iterator()
        val commitmentInputs = try {
            request.credentialStatements.map { statement ->
                val account = accountIterator.next()
                val attributeRandomness = randomnessMap[account.address]
                val identity = getIdentity(account)

                if (attributeRandomness == null) {
                    Log.e("Attribute randomness is not available for account ${account.address}")
                    mutableEventsFlow.tryEmit(
                        Event.ShowFloatingError(
                            Error.InternalError
                        )
                    )
                    return
                }
                if (identity == null) {
                    Log.e("Identity is not available for account ${account.address}")
                    mutableEventsFlow.tryEmit(
                        Event.ShowFloatingError(
                            Error.InternalError
                        )
                    )
                    return
                }

                val identityProviderIndex = identity.identityProviderId
                val randomness: MutableMap<AttributeType, String> = mutableMapOf()
                val attributeValues: MutableMap<AttributeType, String> = mutableMapOf()

                statement.statement.forEach { stat ->
                    val attributeTag = stat.attributeTag
                    val attributeType = AttributeType.fromJSON(attributeTag)
                    randomness[attributeType] = attributeRandomness.attributesRand[attributeTag]!!
                    attributeValues[attributeType] =
                        identity.identityObject!!.attributeList.chosenAttributes[attributeTag]!!
                }

                AccountCommitmentInput.builder()
                    .issuer(UInt32.from(identityProviderIndex))
                    .values(attributeValues)
                    .randomness(randomness)
                    .build()
            }
        } catch (e: Exception) {
            Log.e("Failed to build commitment inputs", e)
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InternalError
                )
            )
            return
        }

        val network = if (BuildConfig.ENV_NAME == "production") Network.MAINNET else Network.TESTNET
        accountIterator = chosenAccounts.iterator()
        val qualifiedRequest = try {
            request.qualify { stat ->
                stat.qualify(
                    CredentialRegistrationId.from(accountIterator.next().credential!!.getCredId()!!),
                    network
                )
            }
        } catch (e: Exception) {
            Log.e("Failed to qualify request.", e)
            mutableEventsFlow.tryEmit(
                Event.ShowFloatingError(
                    Error.InternalError
                )
            )
            return
        }

        ProxyRepository().getIGlobalInfo({ globalInfo ->
            val cryptographicParams: GlobalParams = globalInfo.value

            val input = Web3IdProofInput.builder()
                .request(qualifiedRequest)
                .commitmentInputs(commitmentInputs)
                .globalContext(
                    CryptographicParameters.builder()
                        .genesisString(cryptographicParams.genesisString)
                        .bulletproofGenerators(BulletproofGenerators.from(cryptographicParams.bulletproofGenerators))
                        .onChainCommitmentKey(PedersenCommitmentKey.from(cryptographicParams.onChainCommitmentKey))
                        .build()
                )
                .build()

            try {
                val proof = Web3IdProof.getWeb3IdProof(input)

                val wrappedProof = VerifiablePresentationWrapper(
                    verifiablePresentationJson = proof
                )
                respondSuccess(result = Gson().toJson(wrappedProof))

                mutableStateFlow.tryEmit(State.Idle)
                handleNextOldestPendingSessionRequest()
            } catch (e: Exception) {
                Log.e("failed_creating_verifiable_presentation", e)
                respondError(
                    message = "Unable to create verifiable presentation: internal error"
                )
                mutableEventsFlow.tryEmit(
                    Event.ShowFloatingError(
                        Error.CryptographyFailed
                    )
                )
            }
        },
            {
                Log.e("Failed to retrieve global cryptographic parameters", it)
                mutableEventsFlow.tryEmit(
                    Event.ShowFloatingError(
                        Error.LoadingFailed
                    )
                )
            })
    }

    fun getIdentity(account: Account): Identity? {
        return sessionRequestIdentityProofIdentities[account.identityId]
    }

    fun getProofProvableState(): ProofProvableState {
        return sessionRequestIdentityProofProvable
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
        val accountTransactionPayload = sessionRequestAccountTransactionPayload

        val accountTransactionInput = CreateAccountTransactionInput(
            expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000,
            from = sessionRequestAccount.address,
            keys = accountKeys,
            nonce = sessionRequestTransactionNonce.nonce,
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
                    "switching_back_from_account_selection"
                )

                mutableStateFlow.tryEmit(
                    if (state.forIdentityProof)
                        createIdentityProofRequestState(state.identityProofPosition!!)
                    else
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

            class IdentityProofRequestReview(
                val request: UnqualifiedRequest,
                val chosenAccounts: List<Account>,
                val currentStatement: Int,
                provable: ProofProvableState,
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
        object AccountMismatch : Error

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
    }

    /**
     * Wrapper for sending the verifiable presentation as JSON over WalletConnect. This is
     * required to prevent WalletConnect from automatically parsing the JSON as an object
     * on the dApp side.
     */
    private class VerifiablePresentationWrapper(
        val verifiablePresentationJson: String
    )

    /**
     * Wrapper for receiving parameters as JSON over WalletConnect. This is required to allow a
     * dApp to send bigint values from the Javascript side.
     */
    private class WalletConnectParamsWrapper(
        val paramsJson: String
    )

    private companion object {
        private const val WC_URI_PREFIX = "wc:"
        private const val WC_URI_REQUEST_ID_PARAM = "requestId"

        private const val DEFAULT_ERROR_RESPONSE_CODE = 500

        private const val REQUEST_METHOD_SIGN_AND_SEND_TRANSACTION = "sign_and_send_transaction"
        private const val REQUEST_METHOD_SIGN_MESSAGE = "sign_message"
        private const val REQUEST_METHOD_VERIFIABLE_PRESENTATION = "request_verifiable_presentation"
    }
}
