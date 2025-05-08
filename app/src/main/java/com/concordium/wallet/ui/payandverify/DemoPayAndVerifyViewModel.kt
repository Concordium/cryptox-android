package com.concordium.wallet.ui.payandverify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.sdk.crypto.bulletproof.BulletproofGenerators
import com.concordium.sdk.crypto.pedersencommitment.PedersenCommitmentKey
import com.concordium.sdk.crypto.wallet.Network
import com.concordium.sdk.crypto.wallet.web3Id.AccountCommitmentInput
import com.concordium.sdk.crypto.wallet.web3Id.Statement.IdentityQualifier
import com.concordium.sdk.crypto.wallet.web3Id.UnqualifiedRequest
import com.concordium.sdk.crypto.wallet.web3Id.Web3IdProof
import com.concordium.sdk.crypto.wallet.web3Id.Web3IdProofInput
import com.concordium.sdk.responses.accountinfo.credential.AttributeType
import com.concordium.sdk.responses.cryptographicparameters.CryptographicParameters
import com.concordium.sdk.transactions.AccountTransaction
import com.concordium.sdk.transactions.CCDAmount
import com.concordium.sdk.transactions.CredentialRegistrationId
import com.concordium.sdk.transactions.Expiry
import com.concordium.sdk.transactions.Parameter
import com.concordium.sdk.transactions.ReceiveName
import com.concordium.sdk.transactions.SignerEntry
import com.concordium.sdk.transactions.TransactionFactory
import com.concordium.sdk.transactions.TransactionSigner
import com.concordium.sdk.transactions.UpdateContract
import com.concordium.sdk.types.AccountAddress
import com.concordium.sdk.types.ContractAddress
import com.concordium.sdk.types.Nonce
import com.concordium.sdk.types.UInt32
import com.concordium.sdk.types.UInt64
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.InMemoryCookieJar
import com.concordium.wallet.data.backend.ModifyHeaderInterceptor
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.AttributeRandomness
import com.concordium.wallet.data.cryptolib.SerializeTokenTransferParametersInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.TransactionCost
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.walletconnect.getIdentityObject
import com.concordium.wallet.util.toHex
import com.google.gson.JsonObject
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import com.reown.util.hexToBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DemoPayAndVerifyViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private var isInitialized = false
    private lateinit var invoiceBackend: DemoPayAndVerifyInvoiceBackend
    private val network =
        if (BuildConfig.ENV_NAME.equals("production", ignoreCase = true))
            Network.MAINNET
        else
            Network.TESTNET

    private val accountRepository: AccountRepository by lazy {
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    }
    private val proxyRepository: ProxyRepository by lazy {
        ProxyRepository()
    }
    private val _invoice: MutableStateFlow<DemoPayAndVerifyInvoice?> = MutableStateFlow(null)
    val invoice = _invoice.asStateFlow()
    private val _selectedAccount: MutableStateFlow<DemoPayAndVerifyAccount?> =
        MutableStateFlow(null)
    val selectedAccount = _selectedAccount.asStateFlow()
    private val balancesByAccountAddress: MutableStateFlow<Map<String, BigInteger>?> =
        MutableStateFlow(null)
    val fee: MutableStateFlow<TransactionCost?> = MutableStateFlow(null)
    private val nonce: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val globalParams: MutableStateFlow<GlobalParams?> = MutableStateFlow(null)
    private val _events: MutableSharedFlow<Event> = MutableSharedFlow(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()
    private val isSubmittingPayment: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _paymentResult: MutableStateFlow<PaymentResult?> = MutableStateFlow(null)
    val paymentResult = _paymentResult.asStateFlow()

    val selectedAccountErrors: StateFlow<List<SelectedAccountError>> =
        combine(
            selectedAccount.filterNotNull(),
            invoice.filterNotNull(),
            transform = ::Pair,
        )
            .map { (selectedAccount, invoice) ->
                buildList {
                    val cis2PaymentDetails =
                        invoice.paymentDetails as DemoPayAndVerifyInvoice.PaymentDetails.Cis2
                    if (selectedAccount.balance < cis2PaymentDetails.amount) {
                        add(SelectedAccountError.InsufficientBalance)
                    }

                    val unqualifiedRequest = UnqualifiedRequest.fromJson(invoice.proofRequestJson)
                    if (!isValidIdentityForRequest(selectedAccount.identity, unqualifiedRequest)) {
                        add(
                            SelectedAccountError.Underage(
                                minAgeYears = invoice.minAgeYears,
                            )
                        )
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isLoading: StateFlow<Boolean> =
        combine(
            selectedAccount.map { it == null },
            invoice.map { it == null },
            fee.map { it == null },
            nonce.map { it == null },
            globalParams.map { it == null },
            isSubmittingPayment,
            transform = { values ->
                values.any { it }
            },
        )
            .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val canPayAndVerify: StateFlow<Boolean> =
        combine(
            selectedAccount.map { it != null },
            invoice.map { it != null },
            fee.map { it != null },
            nonce.map { it != null },
            globalParams.map { it != null },
            selectedAccountErrors.map { it.isEmpty() },
            isSubmittingPayment.map { !it },
            paymentResult.map { it == null },
            transform = { values ->
                values.all { it }
            }
        )
            .stateIn(viewModelScope, SharingStarted.Lazily, true)

    init {
        viewModelScope.launch {
            combine(
                _selectedAccount.filterNotNull(),
                _invoice.filterNotNull(),
                transform = ::Pair,
            )
                .collectLatest { (selectedAccount, invoice) ->
                    awaitAll(
                        async {
                            loadPaymentTransactionData(
                                senderAccountAddress = selectedAccount.account.address,
                                cis2PaymentDetails = invoice.paymentDetails as DemoPayAndVerifyInvoice.PaymentDetails.Cis2,
                            )
                        },
                        async {
                            loadProofData()
                        },
                    )
                }
        }

        viewModelScope.launch {
            _invoice
                .filterNotNull()
                .collectLatest { invoice ->
                    loadBalances(invoice)
                }
        }

        // Select the active account once balances are loaded.
        viewModelScope.launch {
            val balances = balancesByAccountAddress
                .filterNotNull()
                .first()
            val invoice = invoice
                .filterNotNull()
                .first()
            val cis2PaymentDetails =
                invoice.paymentDetails as DemoPayAndVerifyInvoice.PaymentDetails.Cis2

            _selectedAccount.emit(
                accountRepository
                    .getAllDoneWithIdentity()
                    .first { it.account.isActive }
                    .let { (account, identity) ->
                        DemoPayAndVerifyAccount(
                            account = account,
                            identity = identity,
                            balance = balances[account.address] ?: BigInteger.ZERO,
                            tokenSymbol = cis2PaymentDetails.tokenSymbol,
                            tokenDecimals = cis2PaymentDetails.tokenDecimals,
                        )
                    }
            )
        }
    }

    fun initializeOnce(
        invoiceUrl: String,
    ) {
        if (isInitialized) {
            return
        }

        viewModelScope.launch {
            loadInvoice(invoiceUrl)
        }

        isInitialized = true
    }

    private suspend fun loadInvoice(
        invoiceUrl: String,
    ) {
        do {
            try {
                invoiceBackend = Retrofit.Builder()
                    .baseUrl(invoiceUrl.trimEnd('/') + "/")
                    .client(
                        OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .cache(null)
                            .cookieJar(InMemoryCookieJar())
                            .addInterceptor(ModifyHeaderInterceptor())
                            .addInterceptor(
                                LoggingInterceptor.Builder()
                                    .setLevel(Level.BASIC)
                                    .log(Platform.INFO)
                                    .request("Request")
                                    .response("Response")
                                    .addHeader("Accept", "application/json")
                                    .addHeader("Content-Type", "application/json")
                                    .build()
                            )
                            .build()
                    )
                    .addConverterFactory(GsonConverterFactory.create(App.appCore.gson))
                    .build()
                    .create(DemoPayAndVerifyInvoiceBackend::class.java)

                val response = invoiceBackend.getInvoice(
                    invoiceUrl = invoiceUrl,
                )

                check(response.version == 1) {
                    "Only V1 invoices are supported"
                }
                check(response.paymentType == "cis2") {
                    "Only CIS-2 invoices are supported"
                }
                check(response.status == "pending") {
                    "The invoice is outdated"
                }

                val invoice = DemoPayAndVerifyInvoice(
                    storeName = invoiceUrl.toHttpUrl().host,
                    minAgeYears = response.minAgeYears,
                    proofRequestJson = response.proofRequestJson,
                    paymentDetails = DemoPayAndVerifyInvoice.PaymentDetails.Cis2(
                        amount = response.cis2Amount!!.toBigInteger(),
                        tokenContractIndex = response.cis2TokenContractIndex!!,
                        tokenSymbol = response.cis2TokenSymbol!!,
                        tokenDecimals = response.cis2TokenDecimals!!,
                        tokenContractName = response.cis2TokenContractName!!,
                        recipientAccountAddress = response.cis2RecipientAccountAddress!!,
                    )
                )

                _invoice.emit(invoice)
            } catch (e: HttpException) {
                e.printStackTrace()

                if (e.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                    _events.emit(
                        Event.FinishWithError(
                            message = "The link is outdated"
                        )
                    )
                    return
                }
            } catch (e: IllegalStateException) {
                if (e is CancellationException) {
                    return
                }

                _events.emit(
                    Event.FinishWithError(
                        message = "Invalid invoice: ${e.message ?: e}"
                    )
                )
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(2000)
        } while (_invoice.value == null)
    }

    private suspend fun loadBalances(
        invoice: DemoPayAndVerifyInvoice,
    ) {
        val cis2PaymentDetails =
            invoice.paymentDetails as DemoPayAndVerifyInvoice.PaymentDetails.Cis2

        var loaded = false

        do {
            try {
                balancesByAccountAddress.emit(
                    accountRepository
                        .getAllDone()
                        .map { account ->
                            viewModelScope.async {
                                val balances = proxyRepository.getCIS2TokenBalanceV1(
                                    index = cis2PaymentDetails.tokenContractIndex.toString(),
                                    subIndex = "0",
                                    accountAddress = account.address,
                                    tokenIds = ""
                                )

                                account.address to balances[0].balance.toBigInteger()
                            }
                        }
                        .awaitAll()
                        .toMap()
                )
                loaded = true
            } catch (e: Exception) {
                if (e is CancellationException) {
                    return
                }

                e.printStackTrace()
            }
            delay(2000)
        } while (!loaded)
    }

    private suspend fun loadPaymentTransactionData(
        senderAccountAddress: String,
        cis2PaymentDetails: DemoPayAndVerifyInvoice.PaymentDetails.Cis2,
    ) {
        nonce.emit(null)
        fee.emit(null)

        do {
            try {
                awaitAll(
                    viewModelScope.async {
                        nonce.emit(
                            proxyRepository
                                .getAccountNonceSuspended(
                                    accountAddress = senderAccountAddress,
                                )
                                .nonce
                        )
                    },
                    viewModelScope.async {
                        fee.emit(
                            getTransactionCost(
                                senderAccountAddress = senderAccountAddress,
                                cis2PaymentDetails = cis2PaymentDetails,
                            )
                        )
                    }
                )
                delay(2000)
            } catch (e: Exception) {
                if (e is CancellationException) {
                    return
                }

                e.printStackTrace()
            }
        } while (nonce.value == null || fee.value == null)
    }

    private suspend fun loadProofData(
    ) {
        globalParams.emit(null)

        do {
            try {
                globalParams.emit(getGlobalParams())
                delay(2000)
            } catch (e: Exception) {
                if (e is CancellationException) {
                    return
                }

                e.printStackTrace()
            }
        } while (globalParams.value == null)
    }

    fun onVerifyAndPayClicked() {
        if (canPayAndVerify.value) {
            _events.tryEmit(Event.Authenticate)
        }
    }

    fun onAuthenticated(
        password: String,
    ) = viewModelScope.launch(Dispatchers.IO) {

        payAndVerify(password)
    }

    private suspend fun payAndVerify(
        password: String,
    ) = try {
        isSubmittingPayment.emit(true)

        val account = selectedAccount.value!!.account
        val identity = selectedAccount.value!!.identity

        val storageAccountDataEncrypted = account.encryptedAccountData
            ?: error("Missing account encrypted data")

        val decryptedJson = App.appCore.auth
            .decrypt(
                password = password,
                encryptedData = storageAccountDataEncrypted,
            )
            ?.let(::String)
            ?: error("Failed decrypting account encrypted data")

        val storageAccountData =
            App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)

        val paymentTransaction: AccountTransaction =
            getPaymentTransaction(
                senderAccountAddress = account.address,
                signer = storageAccountData
                    .accountKeys
                    .getSignerEntry(),
            )

        val paymentTransactionHash = paymentTransaction.hash.asHex()

        val proofJson = getProof(
            identity = identity,
            account = account,
            paymentTransactionHash = paymentTransactionHash,
            attributeRandomness = storageAccountData
                .getAttributeRandomness()
                ?: error("Missing account attribute randomness")
        )

        invoiceBackend
            .payInvoice(
                DemoPayAndVerifyInvoiceBackend.PaymentRequest(
                    proofJson = proofJson,
                    paymentTransactionHex = paymentTransaction.versionedBytes.toHex(),
                )
            )

        _paymentResult.emit(
            PaymentResult.Success(
                transactionHash = paymentTransactionHash,
            )
        )
    } catch (_: CancellationException) {
        // Ok.
    } catch (httpException: HttpException) {
        httpException.printStackTrace()

        _paymentResult.emit(
            PaymentResult.Failure(
                reason = httpException
                    .response()
                    ?.errorBody()
                    ?.string()
                    ?.let { App.appCore.gson.fromJson(it, JsonObject::class.java) }
                    ?.get("title")
                    ?.asString
                    ?: httpException.message()
            )
        )
    } catch (e: Exception) {
        e.printStackTrace()

        _paymentResult.emit(
            PaymentResult.Failure(
                reason = e.message ?: e.toString(),
            )
        )
    } finally {
        isSubmittingPayment.tryEmit(false)
    }

    private fun getPaymentTransaction(
        senderAccountAddress: String,
        signer: SignerEntry,
    ): AccountTransaction {
        val cis2PaymentDetails =
            invoice.value!!.paymentDetails as DemoPayAndVerifyInvoice.PaymentDetails.Cis2
        val nonce = Nonce.from(nonce.value!!.toLong())
        val maxEnergy = UInt64.from(fee.value!!.energy)

        return TransactionFactory
            .newUpdateContract()
            .sender(AccountAddress.from(senderAccountAddress))
            .payload(
                UpdateContract.from(
                    CCDAmount.from(0L),
                    ContractAddress.from(cis2PaymentDetails.tokenContractIndex.toLong(), 0),
                    ReceiveName.from(cis2PaymentDetails.tokenContractName, "transfer"),
                    Parameter.from(
                        getCis2TransferParameter(
                            senderAccountAddress = senderAccountAddress,
                            cis2PaymentDetails = cis2PaymentDetails,
                        )
                    )
                )
            )
            .maxEnergyCost(maxEnergy)
            .nonce(nonce)
            .expiry(Expiry.createNew().addMinutes(10))
            .signer(TransactionSigner.from(signer))
            .build()
    }

    private fun getProof(
        paymentTransactionHash: String,
        account: Account,
        identity: Identity,
        attributeRandomness: AttributeRandomness,
    ): String {
        val globalParams = globalParams.value!!
        val invoice = invoice.value!!

        val unqualifiedRequest = invoice
            .proofRequestJson
            .replace(TX_HASH_PLACEHOLDER, paymentTransactionHash)
            .let(UnqualifiedRequest::fromJson)

        val qualifiedRequest = unqualifiedRequest.qualify { statement ->
            statement.qualify(
                CredentialRegistrationId.from(account.credential!!.getCredId()!!),
                network,
            )
        }

        val commitmentInputs = unqualifiedRequest.credentialStatements.map { statement ->
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
                .issuer(UInt32.from(identity.identityProviderId))
                .values(attributeValues)
                .randomness(randomness)
                .build()
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

        return Web3IdProof.getWeb3IdProof(proofInput)
    }

    private suspend fun getTransactionCost(
        senderAccountAddress: String,
        cis2PaymentDetails: DemoPayAndVerifyInvoice.PaymentDetails.Cis2,
    ): TransactionCost = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getTransferCost(
            type = ProxyRepository.UPDATE,
            memoSize = null,
            amount = BigInteger.ZERO,
            sender = senderAccountAddress,
            contractIndex = cis2PaymentDetails.tokenContractIndex,
            contractSubindex = 0,
            receiveName = cis2PaymentDetails.tokenContractName + ".transfer",
            parameter = getCis2TransferParameter(
                senderAccountAddress = senderAccountAddress,
                cis2PaymentDetails = cis2PaymentDetails,
            ).toHex(),
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private fun getCis2TransferParameter(
        senderAccountAddress: String,
        cis2PaymentDetails: DemoPayAndVerifyInvoice.PaymentDetails.Cis2,
    ): ByteArray = runBlocking {
        App.appCore.cryptoLibrary
            .serializeTokenTransferParameters(
                SerializeTokenTransferParametersInput(
                    tokenId = "",
                    amount = cis2PaymentDetails.amount.toString(),
                    from = senderAccountAddress,
                    to = cis2PaymentDetails.recipientAccountAddress,
                )
            )!!
            .parameter
            .hexToBytes()
    }

    private suspend fun getGlobalParams(
    ) = suspendCancellableCoroutine { continuation ->
        val backendRequest = proxyRepository.getIGlobalInfo(
            success = { continuation.resume(it.value) },
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private fun isValidIdentityForRequest(
        identity: Identity,
        request: UnqualifiedRequest,
    ): Boolean {
        val identityObject = getIdentityObject(identity)
        return request.credentialStatements.all { statement ->
            statement.idQualifier is IdentityQualifier
                    && (statement.idQualifier as IdentityQualifier).issuers.contains(identity.identityProviderId.toLong())
                    && statement.canBeProvedBy(identityObject)
        }
    }

    sealed interface SelectedAccountError {

        object InsufficientBalance : SelectedAccountError

        class Underage(
            val minAgeYears: Int,
        ) : SelectedAccountError
    }

    sealed interface PaymentResult {

        class Failure(
            val reason: String,
        ) : PaymentResult

        class Success(
            val transactionHash: String,
        ) : PaymentResult
    }

    sealed interface Event {

        object Authenticate : Event

        class ShowFloatingError(
            val message: String,
        ) : Event

        class FinishWithError(
            val message: String,
        ) : Event
    }

    private companion object {
        private const val TX_HASH_PLACEHOLDER = "payment-transaction-hash"
    }
}
