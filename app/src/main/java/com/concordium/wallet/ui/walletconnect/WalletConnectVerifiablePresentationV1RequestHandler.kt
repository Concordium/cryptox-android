package com.concordium.wallet.ui.walletconnect

import com.concordium.sdk.crypto.bls.BLSSecretKey
import com.concordium.sdk.crypto.wallet.ConcordiumHdWallet
import com.concordium.sdk.crypto.wallet.Network
import com.concordium.sdk.crypto.wallet.web3Id.GivenContext
import com.concordium.sdk.crypto.wallet.web3Id.IdentityClaims
import com.concordium.sdk.crypto.wallet.web3Id.IdentityClaimsAccountProofInput
import com.concordium.sdk.crypto.wallet.web3Id.IdentityClaimsIdentityProofInput
import com.concordium.sdk.crypto.wallet.web3Id.VerifiablePresentationV1
import com.concordium.sdk.crypto.wallet.web3Id.VerificationRequestAnchor
import com.concordium.sdk.crypto.wallet.web3Id.VerificationRequestV1
import com.concordium.sdk.responses.accountinfo.credential.AttributeType
import com.concordium.sdk.responses.cryptographicparameters.CryptographicParameters
import com.concordium.sdk.serializing.CborMapper
import com.concordium.sdk.serializing.JsonMapper
import com.concordium.sdk.transactions.CredentialRegistrationId
import com.concordium.sdk.types.UInt32
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.PrivateIdObjectData
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.SubmissionStatusResponse
import com.concordium.wallet.data.preferences.WalletSetupPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Error
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.Event
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel.State
import com.concordium.wallet.util.Log
import com.reown.util.hexToBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
    private val walletSetupPreferences: WalletSetupPreferences,
) {
    private val network =
        if (BuildConfig.ENV_NAME.equals("production", ignoreCase = true))
            Network.MAINNET
        else
            Network.TESTNET

    // It doesn't really matter,
    // but every WC request is associated with an account.
    private lateinit var account: Account
    private lateinit var appMetadata: WalletConnectViewModel.AppMetadata
    private lateinit var verificationRequest: VerificationRequestV1
    private lateinit var identityClaims: List<IdentityClaims>
    private lateinit var identityProofProvableState: WalletConnectViewModel.ProofProvableState
    private lateinit var identitiesById: Map<Int, Identity>
    private lateinit var credentialsByClaims: Map<IdentityClaims, IdentityProofRequestSelectedCredential>
    private lateinit var globalParams: CryptographicParameters
    private lateinit var anchorBlockHash: String

    suspend fun start(
        params: String,
        account: Account,
        appMetadata: WalletConnectViewModel.AppMetadata,
    ) {
        try {
            this.verificationRequest = JsonMapper.INSTANCE.readValue(
                params,
                VerificationRequestV1::class.java
            )

            this.identityClaims = verificationRequest
                .subjectClaims
                .filterIsInstance<IdentityClaims>()
                .takeIf { it.size == verificationRequest.subjectClaims.size }
                ?: error("Only requests with identity claims are supported")

            check(identityClaims.isNotEmpty()) {
                "There are no claims"
            }

            this.identitiesById = identityRepository.getAllDone()
                .associateBy(Identity::id)

            this.credentialsByClaims = identityClaims.associateWith { claims ->
                when {
                    claims.canUseAccount ->
                        IdentityProofRequestSelectedCredential.Account(
                            account = account,
                            identity = getIdentity(account),
                        )

                    claims.canUseIdentity ->
                        IdentityProofRequestSelectedCredential.Identity(
                            identity = getIdentity(account),
                        )

                    else ->
                        error("Only identity claims with account or identity source are supported")

                }
            }
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

        this.account = account
        this.appMetadata = appMetadata

        // TODO Actually set the state
        this.identityProofProvableState = WalletConnectViewModel.ProofProvableState.Provable

        loadDataAndPresentReview()
    }

    private suspend fun loadDataAndPresentReview() = coroutineScope {
        try {
            awaitAll(
                async {
                    this@WalletConnectVerifiablePresentationV1RequestHandler.globalParams =
                        getGlobalParams()
                            .toSdkCryptographicParameters()
                },
                async {
                    this@WalletConnectVerifiablePresentationV1RequestHandler.anchorBlockHash =
                        getVerifiedAnchorBlockHash()
                }
            )
        } catch (e: Exception) {
            Log.e("Failed loading necessary data", e)

            respondError("Failed loading necessary data: $e")

            emitEvent(
                Event.ShowFloatingError(
                    Error.LoadingFailed
                )
            )

            onFinish()

            return@coroutineScope
        }

        emitState(
            createIdentityProofRequestState(
                currentClaimIndex = 0,
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

    private suspend fun getVerifiedAnchorBlockHash(
    ): String = withContext(Dispatchers.IO) {
        var requestAnchorTransaction: SubmissionStatusResponse? = null

        // For now, the wallet needs to wait for a transaction to be included into a block.
        // Not necessarily finalized.
        for (attempt in (1..3)) {
            Log.d("Attempt #$attempt to get the anchor transaction")

            delay(3000)

            requestAnchorTransaction = runCatching {
                proxyRepository
                    .getSubmissionStatus(verificationRequest.transactionRef.asHex())
                    .takeUnless { it.blockHashes.isNullOrEmpty() }
            }.getOrNull()

            if (requestAnchorTransaction != null) {
                break
            }
        }

        val blockHash = requestAnchorTransaction
            ?.blockHashes
            ?.first()
            ?: error("Failed getting anchor transaction block hash in time")

        val anchorCborHex = requestAnchorTransaction.registeredData
            ?: error("Anchor transaction data is missing")

        val anchor = CborMapper.INSTANCE.readValue(
            anchorCborHex.hexToBytes(),
            VerificationRequestAnchor::class.java
        )

        check(verificationRequest.verifyAnchor(anchor)) {
            "Anchor doesn't match the request"
        }

        return@withContext blockHash
    }

    suspend fun onAuthorizedForApproval(
        password: String,
    ) = withContext(Dispatchers.Default) {
        try {

        } catch (e: Exception) {
            Log.e("Failed getting attribute randomness", e)

            respondError("Failed getting attribute randomness: $e")

            emitEvent(
                Event.ShowFloatingError(
                    Error.InternalError
                )
            )

            return@withContext
        }

        try {
            // Both file- and seed-based IDs can do V1 proofs,
            // the keys are either derived or decrypted.

            val hdWallet: ConcordiumHdWallet? =
                walletSetupPreferences
                    .takeIf(WalletSetupPreferences::hasEncryptedSeed)
                    ?.let { walletSetupPreferences ->
                        ConcordiumHdWallet.fromHex(
                            walletSetupPreferences.getSeedHex(password),
                            network
                        )
                    }

            val proofInputs = credentialsByClaims
                .map { (identityClaims, credential) ->
                    when (credential) {
                        is IdentityProofRequestSelectedCredential.Account ->
                            getAccountProofInput(
                                identityClaims = identityClaims,
                                account = credential.account,
                                identity = credential.identity,
                                password = password,
                            )

                        is IdentityProofRequestSelectedCredential.Identity ->
                            getIdentityProofInput(
                                identityClaims = identityClaims,
                                identity = credential.identity,
                                password = password,
                                hdWallet = hdWallet,
                            )
                    }
                }

            val providedContext =
                verificationRequest
                    .context
                    .requested
                    .map { requestedInfoLabel ->
                        val info = when (requestedInfoLabel) {
                            GivenContext.BLOCK_HASH_LABEL ->
                                anchorBlockHash

                            GivenContext.RESOURCE_ID_LABEL ->
                                "buyer need midnight amateur mix jungle top odor mouse exotic master strong"

                            else ->
                                error("Can't provide $requestedInfoLabel")
                        }
                        GivenContext(requestedInfoLabel, info)
                    }

            val proof = VerifiablePresentationV1.getVerifiablePresentation(
                verificationRequest,
                proofInputs,
                providedContext,
                globalParams
            )

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

            onFinish()
        }
    }

    private suspend fun getIdentityProofInput(
        identityClaims: IdentityClaims,
        identity: Identity,
        password: String,
        hdWallet: ConcordiumHdWallet?,
    ): IdentityClaimsIdentityProofInput = withContext(Dispatchers.Default) {
        val identityProvider = identity.identityProvider

        val idCredSec: BLSSecretKey
        val prfKey: BLSSecretKey
        val signatureBlindingRandomness: String

        when {
            hdWallet != null -> {
                idCredSec = hdWallet.getIdCredSec(
                    identityProvider.ipInfo.ipIdentity,
                    identity.identityIndex
                )
                prfKey = hdWallet.getPrfKey(
                    identityProvider.ipInfo.ipIdentity,
                    identity.identityIndex
                )
                signatureBlindingRandomness = hdWallet.getSignatureBlindingRandomness(
                    identityProvider.ipInfo.ipIdentity,
                    identity.identityIndex
                )
            }

            identity.privateIdObjectDataEncrypted != null -> {
                val privateData: PrivateIdObjectData =
                    App
                        .appCore
                        .auth
                        .decrypt(
                            password = password,
                            encryptedData = identity.privateIdObjectDataEncrypted!!,
                        )
                        ?.let { decrytpedBytes ->
                            App.appCore.gson.fromJson(
                                String(decrytpedBytes),
                                PrivateIdObjectData::class.java
                            )
                        }
                        ?: error("Can't decrypt identity private data")

                signatureBlindingRandomness = privateData.randomness
                prfKey =
                    privateData
                        .aci
                        .prfKey
                        .let(BLSSecretKey::from)
                idCredSec =
                    privateData
                        .aci
                        .credentialHolderInformation
                        .idCredSecret
                        .let(BLSSecretKey::from)

            }

            else -> {
                error("Keys needed for identity proof can't be neither derived nor decrypted")
            }
        }

        val ipInfo =
            identityProvider
                .ipInfo
                .toSdkIdentityProviderInfo()

        val arsInfos =
            identityProvider
                .arsInfos
                .mapValues { (_, it) -> it.toSdkAnonymityRevokerInfo() }

        val identityObject =
            identity
                .identityObject!!
                .toSdkIdentityObject()

        return@withContext identityClaims.getIdentityProofInput(
            network,
            ipInfo,
            arsInfos,
            identityObject,
            idCredSec,
            prfKey,
            signatureBlindingRandomness,
        )
    }

    private suspend fun getAccountProofInput(
        identityClaims: IdentityClaims,
        account: Account,
        identity: Identity,
        password: String,
    ): IdentityClaimsAccountProofInput = withContext(Dispatchers.Default) {

        val storageAccountDataEncrypted = account.encryptedAccountData
            ?: error("Account has no encrypted data")

        val decryptedJson = App.appCore.auth
            .decrypt(
                password = password,
                encryptedData = storageAccountDataEncrypted
            )
            ?.let(::String)
            ?: error("Failed to decrypt the account data")

        val ipIdentity =
            identity
                .identityProvider
                .ipInfo
                .ipIdentity
                .let(UInt32::from)

        val credId =
            account
                .credential!!
                .getCredId()!!
                .let(CredentialRegistrationId::from)

        val attributeValues =
            identity
                .identityObject!!
                .toSdkIdentityObject()
                .attributeList
                .chosenAttributes

        val attributeRandomness: Map<AttributeType, String> =
            App.appCore.gson
                .fromJson(decryptedJson, StorageAccountData::class.java)
                .getAttributeRandomness()
                ?.attributesRand
                ?.mapKeys { (typeString, _) ->
                    AttributeType.fromJSON(typeString)
                }
                ?: error("Attribute randomness is not available for the account")

        return@withContext identityClaims.getAccountProofInput(
            network,
            ipIdentity,
            credId,
            attributeValues,
            attributeRandomness,
        )
    }

    private fun createIdentityProofRequestState(
        currentClaimIndex: Int,
    ): State.SessionRequestReview =
        State.SessionRequestReview.IdentityProofRequestReview(
            connectedAccount = account,
            appMetadata = appMetadata,
            claims =
            identityClaims
                .map { claims ->
                    IdentityProofRequestClaims(
                        statements = claims.statements,
                        selectedCredential = credentialsByClaims.getValue(claims),
                        canSelectAccounts = claims.canUseAccount,
                        canSelectIdentities = claims.canUseIdentity,
                    )
                },
            currentClaim = currentClaimIndex,
            provable = identityProofProvableState,
            isV1 = true,
        )

    private fun getIdentity(account: Account) =
        identitiesById.getValue(account.identityId)

    private val IdentityClaims.canUseIdentity: Boolean
        get() = source.contains(IdentityClaims.IDENTITY_CREDENTIAL_SOURCE)

    private val IdentityClaims.canUseAccount: Boolean
        get() = source.contains(IdentityClaims.ACCOUNT_CREDENTIAL_SOURCE)

    companion object {
        const val METHOD = "request_verifiable_presentation_v1"
    }
}
