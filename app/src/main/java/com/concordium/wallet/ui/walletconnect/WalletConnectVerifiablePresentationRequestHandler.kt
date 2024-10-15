package com.concordium.wallet.ui.walletconnect

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
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.AttributeRandomness
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Error
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Event
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.State
import com.concordium.wallet.util.KeyCreationVersion
import com.concordium.wallet.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WalletConnectVerifiablePresentationRequestHandler(
    private val respondSuccess: (result: String) -> Unit,
    private val respondError: (message: String) -> Unit,
    private val emitEvent: (event: Event) -> Unit,
    private val emitState: (state: State) -> Unit,
    private val onFinish: () -> Unit,
    private val proxyRepository: ProxyRepository,
    private val identityRepository: IdentityRepository,
    private val keyCreationVersion: KeyCreationVersion,
) {
    private val network =
        if (BuildConfig.ENV_NAME.equals("production", ignoreCase = true))
            Network.MAINNET
        else
            Network.TESTNET

    private lateinit var appMetadata: WalletConnectViewModel.AppMetadata
    private lateinit var identityProofRequest: UnqualifiedRequest
    private lateinit var identityProofProvableState: WalletConnectViewModel.ProofProvableState
    private lateinit var identitiesById: Map<Int, Identity>
    private lateinit var accountsPerStatement: MutableList<Account>
    private lateinit var globalParams: GlobalParams

    suspend fun start(
        params: String,
        account: Account,
        availableAccounts: Collection<Account>,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        if (!keyCreationVersion.useV1) {
            Log.d("A non-seed phrase wallet does not support identity proofs")

            respondError("A non-seed phrase wallet does not support identity proofs")

            emitEvent(
                Event.ShowFloatingError(
                    Error.NotSeedPhraseWalletError
                )
            )

            onFinish()

            return
        }

        try {
            val wrappedParams =
                App.appCore.gson.fromJson(params, WalletConnectParamsWrapper::class.java)
            this.identityProofRequest = UnqualifiedRequest.fromJson(wrappedParams.paramsJson)
        } catch (e: Exception) {
            onInvalidRequest("Failed to parse identity proof request parameters: $params", e)
            return
        }

        if (identityProofRequest.credentialStatements.any { it.statementType == StatementType.SmartContract }) {
            onInvalidRequest("Web3Id statements are not supported.")
            return
        }

        this.appMetadata = appMetadata

        try {
            // Check that the statement is acceptable. Using the session account here to qualify
            // is sufficient because we don't check that the statement is provable.
            val credId = CredentialRegistrationId.from(account.credential!!.getCredId()!!)
            AcceptableRequest.acceptableRequest(identityProofRequest.qualify { stat ->
                stat.qualify(credId, network)
            })
        } catch (e: Exception) {
            onInvalidRequest("Request received is not acceptable", e)
            return
        }

        this.identitiesById = identityRepository.getAllDone()
            .associateBy(Identity::id)

        val orderedAccounts = listOf(account) + availableAccounts
        val suitableAccountsPerStatement =
            identityProofRequest.credentialStatements.map { statement ->
                // Prefer session account,
                // otherwise find any account that can prove the statement.
                orderedAccounts.find { account ->
                    isValidIdentityForStatement(
                        identity = getIdentity(account)!!,
                        statement = statement,
                    )
                }
            }

        if (suitableAccountsPerStatement.any { it == null }) {
            // Check whether any statement does not have any account, which issuer is allowed.
            val noCompatibleIssuers =
                identityProofRequest.credentialStatements.any { statement ->
                    availableAccounts.none { account ->
                        (statement.idQualifier as IdentityQualifier).issuers.contains(
                            getIdentity(account)!!.identityProviderId.toLong()
                        )
                    }
                }

            this.identityProofProvableState =
                if (noCompatibleIssuers)
                    WalletConnectViewModel.ProofProvableState.NoCompatibleIssuer
                else
                    WalletConnectViewModel.ProofProvableState.UnProvable

            this.accountsPerStatement =
                suitableAccountsPerStatement.map { it ?: account }.toMutableList()
        } else {
            this.identityProofProvableState = WalletConnectViewModel.ProofProvableState.Provable
            this.accountsPerStatement =
                suitableAccountsPerStatement.requireNoNulls().toMutableList()
        }

        loadDataAndPresentReview()
    }

    private suspend fun loadDataAndPresentReview() {
        try {
            this.globalParams = getGlobalParams()
        } catch (e: Exception) {
            Log.e("Failed loading global params", e)

            respondError("Failed loading global params")

            emitEvent(
                Event.ShowFloatingError(
                    Error.LoadingFailed
                )
            )

            onFinish()

            return
        }

        emitState(
            createIdentityProofRequestState(
                currentStatementIndex = 0,
            )
        )
    }

    private suspend fun getGlobalParams(
    ) = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getIGlobalInfo(
            success = { continuation.resume(it.value) },
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }


    suspend fun onAuthorizedForApproval(
        password: String,
    ) {
        val attributeRandomnessByAccount: Map<String, AttributeRandomness> = try {
            accountsPerStatement.associateBy(Account::address) { account ->
                val storageAccountDataEncrypted = account.encryptedAccountData
                    ?: error("Account has no encrypted data")

                val decryptedJson = App.appCore.auth
                    .decrypt(
                        password = password,
                        encryptedData = storageAccountDataEncrypted
                    )
                    ?.let(::String)
                    ?: error("Failed to decrypt the account data")

                val storageAccountData =
                    App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)

                checkNotNull(storageAccountData.getAttributeRandomness()) {
                    "Attribute randomness is not available for account ${account.address}"
                }
            }
        } catch (e: Exception) {
            Log.e("Failed getting attribute randomness", e)

            emitEvent(
                Event.ShowFloatingError(
                    Error.InternalError
                )
            )

            return
        }

        val commitmentInputs = try {
            val accountIterator = accountsPerStatement.iterator()
            identityProofRequest.credentialStatements.map { statement ->
                val statementAccount = accountIterator.next()
                val attributeRandomness =
                    attributeRandomnessByAccount.getValue(statementAccount.address)
                val statementIdentity = getIdentity(statementAccount)!!
                val identityProviderIndex = statementIdentity.identityProviderId
                val randomness: MutableMap<AttributeType, String> = mutableMapOf()
                val attributeValues: MutableMap<AttributeType, String> = mutableMapOf()

                statement.statement.forEach { stat ->
                    val attributeTag = stat.attributeTag
                    val attributeType = AttributeType.fromJSON(attributeTag)
                    randomness[attributeType] = attributeRandomness.attributesRand[attributeTag]!!
                    attributeValues[attributeType] =
                        statementIdentity.identityObject!!.attributeList.chosenAttributes[attributeTag]!!
                }

                AccountCommitmentInput.builder()
                    .issuer(UInt32.from(identityProviderIndex))
                    .values(attributeValues)
                    .randomness(randomness)
                    .build()
            }
        } catch (e: Exception) {
            Log.e("Failed to build commitment inputs", e)

            emitEvent(
                Event.ShowFloatingError(
                    Error.InternalError
                )
            )

            return
        }

        val qualifiedRequest = try {
            val accountIterator = accountsPerStatement.iterator()
            identityProofRequest.qualify { stat ->
                val credId =
                    CredentialRegistrationId.from(accountIterator.next().credential!!.getCredId()!!)
                stat.qualify(credId, network)
            }
        } catch (e: Exception) {
            Log.e("Failed to qualify request", e)

            emitEvent(
                Event.ShowFloatingError(
                    Error.InternalError
                )
            )

            return
        }

        val proofInput = Web3IdProofInput.builder()
            .request(qualifiedRequest)
            .commitmentInputs(commitmentInputs)
            .globalContext(
                CryptographicParameters.builder()
                    .genesisString(globalParams.genesisString)
                    .bulletproofGenerators(BulletproofGenerators.from(globalParams.bulletproofGenerators))
                    .onChainCommitmentKey(PedersenCommitmentKey.from(globalParams.onChainCommitmentKey))
                    .build()
            )
            .build()

        try {
            val proof = Web3IdProof.getWeb3IdProof(proofInput)
            val wrappedProof = VerifiablePresentationWrapper(proof)

            respondSuccess(App.appCore.gson.toJson(wrappedProof))

            onFinish()
        } catch (e: Exception) {
            Log.e("Failed creating verifiable presentation", e)

            respondError("Unable to create verifiable presentation: internal error")

            emitEvent(
                Event.ShowFloatingError(
                    Error.InternalError
                )
            )
        }
    }

    fun onChangeAccountClicked(
        statementIndex: Int,
        availableAccounts: Collection<Account>,
    ) {
        val statement = identityProofRequest.credentialStatements[statementIndex]


        val validAccounts = availableAccounts.filter { account ->
            getIdentity(account)
                ?.let { isValidIdentityForStatement(it, statement) }
                ?: false
        }

        if (validAccounts.size > 1) {
            Log.d(
                "switching_to_account_selection:" +
                        "\nvalidAccounts=${availableAccounts.size}"
            )

            emitState(
                State.AccountSelection(
                    selectedAccount = accountsPerStatement[statementIndex],
                    accounts = validAccounts,
                    appMetadata = appMetadata,
                    identityProofPosition = statementIndex,
                )
            )
        } else {
            Log.w("no_accounts_to_select_from")
        }
    }

    fun onAccountSelected(
        statementIndex: Int,
        account: Account,
    ) {
        accountsPerStatement[statementIndex] = account

        emitState(
            createIdentityProofRequestState(statementIndex)
        )
    }

    fun onAccountSelectionBackPressed(
        statementIndex: Int,
    ) {
        emitState(
            createIdentityProofRequestState(statementIndex)
        )
    }

    fun getIdentity(account: Account) =
        identitiesById[account.identityId]

    private fun isValidIdentityForStatement(
        identity: Identity,
        statement: UnqualifiedRequestStatement
    ): Boolean =
        statement.idQualifier is IdentityQualifier
                && (statement.idQualifier as IdentityQualifier).issuers.contains(identity.identityProviderId.toLong())
                && statement.canBeProvedBy(getIdentityObject(identity))

    private fun onInvalidRequest(responseMessage: String, e: Exception? = null) {
        if (e == null) Log.e(responseMessage) else Log.e(responseMessage, e)

        respondError(if (e == null) responseMessage else "$responseMessage: $e")

        emitEvent(
            Event.ShowFloatingError(
                Error.InvalidRequest
            )
        )

        onFinish()
    }

    private fun createIdentityProofRequestState(
        currentStatementIndex: Int,
    ): State.SessionRequestReview =
        State.SessionRequestReview.IdentityProofRequestReview(
            connectedAccount = accountsPerStatement.first(),
            appMetadata = appMetadata,
            request = identityProofRequest,
            chosenAccounts = accountsPerStatement,
            currentStatement = currentStatementIndex,
            provable = identityProofProvableState
        )

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
}
