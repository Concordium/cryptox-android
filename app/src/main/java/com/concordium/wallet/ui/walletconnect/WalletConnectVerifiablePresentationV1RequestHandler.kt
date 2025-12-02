package com.concordium.wallet.ui.walletconnect

import com.concordium.sdk.crypto.wallet.Network
import com.concordium.sdk.crypto.wallet.web3Id.Statement.IdentityQualifier
import com.concordium.sdk.crypto.wallet.web3Id.Statement.UnqualifiedRequestStatement
import com.concordium.sdk.crypto.wallet.web3Id.VerificationRequestAnchor
import com.concordium.sdk.crypto.wallet.web3Id.VerificationRequestV1
import com.concordium.sdk.serializing.CborMapper
import com.concordium.sdk.serializing.JsonMapper
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Error
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Event
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.State
import com.concordium.wallet.util.Log
import com.reown.util.hexToBytes
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WalletConnectVerifiablePresentationV1RequestHandler(
    private val respondSuccess: (result: String) -> Unit,
    private val respondError: (message: String) -> Unit,
    private val emitEvent: (event: Event) -> Unit,
    private val emitState: (state: State) -> Unit,
    private val onFinish: () -> Unit,
    private val proxyRepository: ProxyRepository,
    private val identityRepository: IdentityRepository,
    private val activeWalletType: AppWallet.Type,
) {
    private val network =
        if (BuildConfig.ENV_NAME.equals("production", ignoreCase = true))
            Network.MAINNET
        else
            Network.TESTNET

    private lateinit var appMetadata: WalletConnectViewModel.AppMetadata
    private lateinit var verificationRequest: VerificationRequestV1
    private lateinit var identityProofProvableState: WalletConnectViewModel.ProofProvableState
    private lateinit var identitiesById: Map<Int, Identity>
    private lateinit var accountsPerStatement: MutableList<Account>
    private lateinit var globalParams: GlobalParams
    private lateinit var anchorBlockHash: String

    suspend fun start(
        params: String,
        account: Account,
        availableAccounts: Collection<Account>,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        if (activeWalletType != AppWallet.Type.SEED) {
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
            this.verificationRequest = JsonMapper.INSTANCE.readValue(
                params,
                VerificationRequestV1::class.java
            )
        } catch (e: Exception) {
            Log.e("Failed parsing the request", e)

            respondError("Failed parsing the request: $e")

            emitEvent(
                Event.ShowFloatingError(
                    Error.InvalidRequest
                )
            )

            onFinish()

            return
        }

        this.appMetadata = appMetadata

        this.identitiesById = identityRepository.getAllDone()
            .associateBy(Identity::id)

        this.identityProofProvableState = WalletConnectViewModel.ProofProvableState.Provable

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

        try {
            val requestAnchorTransaction = proxyRepository
                .getSubmissionStatus(verificationRequest.transactionRef.asHex())

            check(requestAnchorTransaction.status == TransactionStatus.FINALIZED) {
                "Anchor transaction is not finalized"
            }

            val anchorCborHex = requestAnchorTransaction.registerData
                ?: error("Anchor transaction data is missing")

            val anchor = CborMapper.INSTANCE.readValue(
                anchorCborHex.hexToBytes(),
                VerificationRequestAnchor::class.java
            )

            check(verificationRequest.verifyAnchor(anchor)) {
                "Anchor doesn't match the request"
            }

            this.anchorBlockHash = requestAnchorTransaction
                .blockHashes
                ?.first()
                ?: error("Anchor transaction block hash is missing")
        } catch (e: Exception) {
            Log.e("Failed checking the anchor", e)

            respondError("Failed checking the anchor: $e")

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
        try {

        } catch (e: Exception) {
            Log.e("Failed getting attribute randomness", e)

            respondError("Failed getting attribute randomness: $e")

            emitEvent(
                Event.ShowFloatingError(
                    Error.InternalError
                )
            )

            return
        }

        try {
            val proof = ""

            respondSuccess(proof)

            onFinish()
        } catch (e: Exception) {
            Log.e("Failed creating verifiable presentation", e)

            respondError("Failed creating verifiable presentation: $e")

            emitEvent(
                Event.ShowFloatingError(
                    Error.InternalError
                )
            )
        }
    }

    private fun isValidIdentityForStatement(
        identity: Identity,
        statement: UnqualifiedRequestStatement,
    ): Boolean =
        statement.idQualifier is IdentityQualifier
                && (statement.idQualifier as IdentityQualifier).issuers.contains(identity.identityProviderId.toLong())
                && statement.canBeProvedBy(getIdentityObject(identity))

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
        val verifiablePresentationJson: String,
    )

    /**
     * Wrapper for receiving parameters as JSON over WalletConnect. This is required to allow a
     * dApp to send bigint values from the Javascript side.
     */
    private class WalletConnectParamsWrapper(
        val paramsJson: String,
    )

    companion object {
        const val METHOD = "request_verifiable_presentation_v1"
    }
}
