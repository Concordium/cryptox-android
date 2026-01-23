package com.concordium.wallet.data.backend.repository

import com.concordium.sdk.transactions.Transaction
import com.concordium.wallet.App
import com.concordium.wallet.core.backend.BackendCallback
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.CIS_2_TOKEN_BALANCE_MAX_TOKEN_IDS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.CIS_2_TOKEN_METADATA_MAX_TOKEN_IDS
import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountKeyData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.AccountTransactions
import com.concordium.wallet.data.model.BakerPoolStatus
import com.concordium.wallet.data.model.CIS2Tokens
import com.concordium.wallet.data.model.CIS2TokensBalances
import com.concordium.wallet.data.model.CIS2TokensMetadata
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.PLTInfo
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.SubmissionStatusResponse
import com.concordium.wallet.data.model.TransactionCost
import com.concordium.wallet.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.math.BigInteger

class ProxyRepository {

    private val backend = App.appCore.getProxyBackend()

    companion object {
        const val SIMPLE_TRANSFER = "simpleTransfer"
        const val ENCRYPTED_TRANSFER = "encryptedTransfer"
        const val TRANSFER_TO_SECRET = "transferToSecret"
        const val TRANSFER_TO_PUBLIC = "transferToPublic"
        const val REGISTER_DELEGATION = "registerDelegation"
        const val UPDATE_DELEGATION = "updateDelegation"
        const val REMOVE_DELEGATION = "removeDelegation"
        const val REGISTER_BAKER = "registerBaker"
        const val UPDATE_BAKER_STAKE = "updateBakerStake"
        const val UPDATE_BAKER_POOL = "updateBakerPool"
        const val UPDATE_BAKER_KEYS = "updateBakerKeys"
        const val REMOVE_BAKER = "removeBaker"
        const val CONFIGURE_BAKER = "configureBaker"
        const val UPDATE = "update"
        const val TOKEN_UPDATE = "tokenUpdate"

        const val CIS_2_TOKEN_BALANCE_MAX_TOKEN_IDS = 20
        const val CIS_2_TOKEN_METADATA_MAX_TOKEN_IDS = 20
    }

    /**
     * Submits [transaction] crafted by the Java SDK
     * through a reliable node behind the wallet-proxy.
     *
     * @see getSubmissionStatus
     */
    suspend fun submitSdkTransaction(
        transaction: Transaction,
    ): SubmissionData =
        backend.submitRawTransaction(
            transactionBytesBody = transaction
                .bytes
                .toRequestBody(
                    contentType = "application/octet-stream".toMediaType(),
                ),
        )

    fun submitCredential(
        credentialWrapper: CredentialWrapper,
        success: (SubmissionData) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<SubmissionData> {
        val call = backend.submitCredential(credentialWrapper)
        call.enqueue(object : BackendCallback<SubmissionData>() {

            override fun onResponseData(response: SubmissionData) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getAccountNonce(
        accountAddress: String,
        success: (AccountNonce) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<AccountNonce> {
        val call = backend.accountNonce(accountAddress)
        call.enqueue(object : BackendCallback<AccountNonce>() {

            override fun onResponseData(response: AccountNonce) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun submitTransfer(
        transfer: CreateTransferOutput,
        success: (SubmissionData) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<SubmissionData> {
        val call = backend.submitTransfer(transfer)
        call.enqueue(object : BackendCallback<SubmissionData>() {

            override fun onResponseData(response: SubmissionData) {
                Log.e("ProxyRepository#submitTransfer Success")
                success(response)
            }

            override fun onFailure(t: Throwable) {
                Log.e("ProxyRepository#submitTransfer Error: ${t.message}")
                failure?.invoke(t)
            }
        })

        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getSubmissionStatus(submissionId: String) =
        backend.submissionStatusSuspended(submissionId)

    fun getSubmissionStatus(
        submissionId: String,
        success: (SubmissionStatusResponse) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<SubmissionStatusResponse> {
        val call = backend.submissionStatus(submissionId)
        call.enqueue(object : BackendCallback<SubmissionStatusResponse>() {

            override fun onResponseData(response: SubmissionStatusResponse) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getTransferCost(
        type: String,
        memoSize: Int? = null,
        amount: BigInteger? = null,
        restake: Boolean? = null,
        passive: Boolean? = null,
        targetChange: Boolean? = null,
        metadataSize: Int? = null,
        openStatus: String? = null,
        suspended: Boolean? = null,
        sender: String? = null,
        contractIndex: Int? = null,
        contractSubindex: Int? = null,
        receiveName: String? = null,
        parameter: String? = null,
        executionNRGBuffer: Int? = null,
        tokenId: String? = null,
        listOperationsSize: Int? = null,
        tokenOperationTypeCount: Map<String, Int>? = null,
        success: (TransactionCost) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<TransactionCost> {
        val call = backend.transferCost(
            type,
            memoSize,
            amount?.toString(),
            restake?.takeIf { it },
            targetChange?.takeIf { it },
            passive?.takeIf { it },
            metadataSize,
            openStatus,
            suspended,
            sender,
            contractIndex,
            contractSubindex,
            receiveName,
            parameter,
            executionNRGBuffer,
            tokenId,
            listOperationsSize,
            App.appCore.gson.toJson(tokenOperationTypeCount),
        )
        call.enqueue(object : BackendCallback<TransactionCost>() {
            override fun onResponseData(response: TransactionCost) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getChainParameters() = backend.chainParameters()

    suspend fun getPassiveDelegationSuspended() = backend.passiveDelegationSuspended()

    fun getBakerPool(
        bakerId: String,
        success: (BakerPoolStatus) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<BakerPoolStatus> {
        val call = backend.bakerPool(bakerId)
        call.enqueue(object : BackendCallback<BakerPoolStatus>() {
            override fun onResponseData(response: BakerPoolStatus) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })
        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getBakerPoolSuspended(poolId: String) = backend.bakerPoolSuspended(poolId)

    suspend fun getAccountBalanceSuspended(accountAddress: String) =
        backend.accountBalanceSuspended(accountAddress)

    fun getAccountBalance(
        accountAddress: String,
        success: (AccountBalance) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<AccountBalance> {
        val call = backend.accountBalance(accountAddress)
        call.enqueue(object : BackendCallback<AccountBalance>() {

            override fun onResponseData(response: AccountBalance) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest<AccountBalance>(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getAccountTransactions(
        accountAddress: String,
        success: (AccountTransactions) -> Unit,
        failure: ((Throwable) -> Unit)?,
        order: String? = "desc",
        from: Int? = null,
        limit: Int? = null,
        includeRewards: String? = "all",
    ): BackendRequest<AccountTransactions> {
        val call = backend.accountTransactions(accountAddress, order, from, limit, includeRewards)
        call.enqueue(object : BackendCallback<AccountTransactions>() {

            override fun onResponseData(response: AccountTransactions) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest<AccountTransactions>(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun requestGTUDrop(
        accountAddress: String,
        success: (SubmissionData) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<SubmissionData> {
        val call = backend.requestGTUDrop(accountAddress)
        call.enqueue(object : BackendCallback<SubmissionData>() {

            override fun onResponseData(response: SubmissionData) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest<SubmissionData>(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getIGlobalInfo(
        success: (GlobalParamsWrapper) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<GlobalParamsWrapper> {
        val call = App.appCore.getProxyBackend().getGlobalInfo()
        call.enqueue(object : BackendCallback<GlobalParamsWrapper>() {

            override fun onResponseData(response: GlobalParamsWrapper) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest<GlobalParamsWrapper>(
            call = call,
            success = success,
            failure = failure
        )
    }

    fun getAccountEncryptedKey(
        accountAddress: String,
        success: (AccountKeyData) -> Unit,
        failure: ((Throwable) -> Unit)?,
    ): BackendRequest<AccountKeyData> {
        val call = App.appCore.getProxyBackend().getAccountEncryptedKey(accountAddress)
        call.enqueue(object : BackendCallback<AccountKeyData>() {

            override fun onResponseData(response: AccountKeyData) {
                success(response)
            }

            override fun onFailure(t: Throwable) {
                failure?.invoke(t)
            }
        })

        return BackendRequest(
            call = call,
            success = success,
            failure = failure
        )
    }

    suspend fun getCIS2Tokens(
        index: String,
        subIndex: String,
        from: String? = null,
        limit: Int? = null,
    ): CIS2Tokens = backend.cis2Tokens(index, subIndex, from, limit)

    /**
     * @param tokenIds comma-separated token IDs, but no more than [CIS_2_TOKEN_METADATA_MAX_TOKEN_IDS]
     *
     * @return metadata items for tokens having it
     */
    suspend fun getCIS2TokenMetadataV1(
        index: String,
        subIndex: String,
        tokenIds: String,
    ): CIS2TokensMetadata = backend.cis2TokenMetadataV1(index, subIndex, tokenIds)

    /**
     * @param tokenIds comma-separated token IDs, but no more than [CIS_2_TOKEN_BALANCE_MAX_TOKEN_IDS]

     * @return balance items for tokens having it
     */
    suspend fun getCIS2TokenBalanceV1(
        index: String,
        subIndex: String,
        accountAddress: String,
        tokenIds: String,
    ): CIS2TokensBalances = backend.cis2TokenBalanceV1(index, subIndex, accountAddress, tokenIds)

    /**
     * @return a list of plt token infos.
     */
    suspend fun getPLTTokens(): List<PLTInfo> = backend.pltTokens()

    /**
     * @return token info and decoded module state
     */
    suspend fun getPLTTokenById(tokenId: String): PLTInfo = backend.getPLTTokenById(tokenId)

    /**
     * @return an object containing a URL that initiate the Transak on-ramp
     */
    suspend fun getTransakWidgetUrl(address: String) = backend.getTransakWidgetUrl(address)
}
