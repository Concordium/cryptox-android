package com.concordium.wallet.data.backend

import com.concordium.wallet.data.cryptolib.CreateTransferOutput
import com.concordium.wallet.data.model.AccountBalance
import com.concordium.wallet.data.model.AccountKeyData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.AccountTransactions
import com.concordium.wallet.data.model.BakerPoolStatus
import com.concordium.wallet.data.model.CIS2Tokens
import com.concordium.wallet.data.model.CIS2TokensBalances
import com.concordium.wallet.data.model.CIS2TokensMetadata
import com.concordium.wallet.data.model.ChainParameters
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.IdentityContainer
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.model.PLTInfo
import com.concordium.wallet.data.model.PassiveDelegation
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.SubmissionStatusResponse
import com.concordium.wallet.data.model.TransactionCost
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ProxyBackend {

    /**
     * Submits a transaction serialized as Block item (version, signature, header, payload).
     */
    @PUT("v0/submitRawTransaction")
    suspend fun submitRawTransaction(@Body transactionBytesBody: RequestBody): SubmissionData

    @PUT("v0/submitCredential")
    fun submitCredential(@Body credential: CredentialWrapper): Call<SubmissionData>

    @GET("v0/bakerPool/{poolId}")
    fun bakerPool(@Path("poolId") poolId: String): Call<BakerPoolStatus>

    @GET("v0/bakerPool/{poolId}")
    suspend fun bakerPoolSuspended(@Path("poolId") poolId: String): Response<BakerPoolStatus>

    @GET("v0/accNonce/{accountAddress}")
    fun accountNonce(@Path("accountAddress") accountAddress: String): Call<AccountNonce>

    @PUT("v0/submitTransfer")
    fun submitTransfer(@Body transfer: CreateTransferOutput): Call<SubmissionData>

    @GET("v0/submissionStatus/{submissionId}")
    fun submissionStatus(@Path("submissionId") submissionId: String): Call<SubmissionStatusResponse>

    @GET("v0/submissionStatus/{submissionId}")
    suspend fun submissionStatusSuspended(@Path("submissionId") submissionId: String): SubmissionStatusResponse

    @GET("v0/transactionCost")
    fun transferCost(
        @Query("type") type: String? = null,
        @Query("memoSize") memoSize: Int? = null,
        @Query("amount") amount: String? = null,
        @Query("restake") restake: Boolean? = null,
        @Query("target") target: Boolean? = null,
        @Query("passive") passive: Boolean? = null,
        @Query("metadataSize") metadataSize: Int? = null,
        @Query("openStatus") openStatus: String? = null,
        @Query("suspended") suspended: Boolean? = null,
        @Query("sender") sender: String? = null,
        @Query("contractIndex") contractIndex: Int? = null,
        @Query("contractSubindex") contractSubindex: Int? = null,
        @Query("receiveName") receiveName: String? = null,
        @Query("parameter") parameter: String? = null,
        @Query("executionNRGBuffer") executionNRGBuffer: Int? = null,
    ): Call<TransactionCost>

    @GET("v0/chainParameters")
    suspend fun chainParameters(): ChainParameters

    @GET("v0/passiveDelegation")
    suspend fun passiveDelegationSuspended(): Response<PassiveDelegation>

    @GET("v1/accBalance/{accountAddress}")
    fun accountBalance(@Path("accountAddress") accountAddress: String): Call<AccountBalance>

//    @GET("v1/accBalance/{accountAddress}")
//    suspend fun accountBalanceSuspended(@Path("accountAddress") accountAddress: String): AccountBalance

    // this hardcoded endpoint is used only for testing purposes
    @GET("v2/accBalance/4GbHu8Ynnt1hc2PGhRAiwGzkXYBxnSCNJEB9dcnGEJPehRw3oo")
    suspend fun accountBalanceSuspended(): AccountBalance

    @GET("v2/accTransactions/{accountAddress}")
    fun accountTransactions(
        @Path("accountAddress") accountAddress: String,
        @Query("order") order: String? = null,
        @Query("from") from: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("includeRewards") includeRewards: String? = null,
    ): Call<AccountTransactions>

    @PUT("v0/testnetGTUDrop/{accountAddress}")
    fun requestGTUDrop(@Path("accountAddress") accountAddress: String): Call<SubmissionData>

    @GET("v2/ip_info")
    fun getV2IdentityProviderInfo(): Call<ArrayList<IdentityProvider>>

    @GET("v0/global")
    fun getGlobalInfo(): Call<GlobalParamsWrapper>

    @GET("v0/global")
    suspend fun getGlobalInfoSuspended(): GlobalParamsWrapper

    @GET("v0/request_id")
    fun requestIdentity(@Query("id_request") idRequest: String): Call<IdentityContainer>

    @GET("v0/accEncryptionKey/{accountAddress}")
    fun getAccountEncryptedKey(@Path("accountAddress") accountAddress: String): Call<AccountKeyData>

    @GET("v0/CIS2Tokens/{index}/{subIndex}")
    suspend fun cis2Tokens(
        @Path("index") index: String,
        @Path("subIndex") subIndex: String,
        @Query("from") from: String? = null,
        @Query("limit") limit: Int? = null,
    ): CIS2Tokens

    @GET("v1/CIS2TokenMetadata/{index}/{subIndex}")
    suspend fun cis2TokenMetadataV1(
        @Path("index") index: String,
        @Path("subIndex") subIndex: String,
        @Query("tokenId") tokenId: String,
    ): CIS2TokensMetadata

    @GET("v1/CIS2TokenBalance/{index}/{subIndex}/{accountAddress}")
    suspend fun cis2TokenBalanceV1(
        @Path("index") index: String,
        @Path("subIndex") subIndex: String,
        @Path("accountAddress") accountAddress: String,
        @Query("tokenId") tokenId: String,
    ): CIS2TokensBalances

    @GET("/v0/plt/tokens")
    suspend fun pltTokens(): List<PLTInfo>

    @GET("/v0/plt/tokenInfo/{tokenId}")
    suspend fun getPLTTokenById(@Path("tokenId") tokenId: String): PLTInfo
}
