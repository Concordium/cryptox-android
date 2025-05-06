package com.concordium.wallet.ui.payandverify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.sdk.cis2.Cis2Transfer
import com.concordium.sdk.cis2.SerializationUtils
import com.concordium.sdk.cis2.TokenAmount
import com.concordium.sdk.cis2.TokenId
import com.concordium.sdk.transactions.Parameter
import com.concordium.sdk.types.AccountAddress
import com.concordium.sdk.types.ContractAddress
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.backend.InMemoryCookieJar
import com.concordium.wallet.data.backend.ModifyHeaderInterceptor
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.TransactionCost
import com.concordium.wallet.util.toHex
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DemoPayAndVerifyViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private var isInitialized = false
    private lateinit var invoiceUrl: String
    private lateinit var invoiceBackend: DemoPayAndVerifyInvoiceBackend

    private val accountRepository: AccountRepository by lazy {
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    }
    private val identityRepository: IdentityRepository by lazy {
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
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
    private val fee: MutableStateFlow<BigInteger?> = MutableStateFlow(null)
    private val nonce: MutableStateFlow<Int?> = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            combine(
                _selectedAccount.filterNotNull(),
                _invoice.filterNotNull(),
                transform = ::Pair,
            )
                .collectLatest { (selectedAccount, invoice) ->
                    loadPaymentTransactionData(
                        senderAccountAddress = selectedAccount.account.address,
                        cis2PaymentDetails = invoice.paymentDetails as DemoPayAndVerifyInvoice.PaymentDetails.Cis2,
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
    }

    fun initializeOnce(
        invoiceUrl: String,
    ) {
        if (isInitialized) {
            return
        }

        this.invoiceUrl = invoiceUrl

        viewModelScope.launch {
            loadInvoice()

            // Select the active account once balances are loaded.
            val balances = balancesByAccountAddress
                .filterNotNull()
                .first()
            _selectedAccount.emit(
                accountRepository
                    .getAllDoneWithIdentity()
                    .first { it.account.isActive }
                    .let { (account, identity) ->
                        DemoPayAndVerifyAccount(
                            account = account,
                            identity = identity,
                            balance = balances[account.address] ?: BigInteger.ZERO,
                        )
                    }
            )
        }

        isInitialized = true
    }

    private suspend fun loadInvoice() {
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
        check(response.version == 1)
        check(response.paymentType == "cis2")

        val invoice = DemoPayAndVerifyInvoice(
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
    }

    private suspend fun loadBalances(
        invoice: DemoPayAndVerifyInvoice,
    ) {
        val cis2PaymentDetails =
            invoice.paymentDetails as DemoPayAndVerifyInvoice.PaymentDetails.Cis2

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
    }

    private suspend fun loadPaymentTransactionData(
        senderAccountAddress: String,
        cis2PaymentDetails: DemoPayAndVerifyInvoice.PaymentDetails.Cis2,
    ) {
        nonce.emit(null)
        fee.emit(null)

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
                        .cost
                )
            }
        )
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
            ).bytes.toHex(),
            success = continuation::resume,
            failure = continuation::resumeWithException,
        )
        continuation.invokeOnCancellation { backendRequest.dispose() }
    }

    private fun getCis2TransferParameter(
        senderAccountAddress: String,
        cis2PaymentDetails: DemoPayAndVerifyInvoice.PaymentDetails.Cis2,
    ): Parameter {
        val transfer = Cis2Transfer(
            TokenId.from(byteArrayOf()),
            TokenAmount.from(cis2PaymentDetails.amount.toString()),
            AccountAddress.from(senderAccountAddress),
            ContractAddress.from(cis2PaymentDetails.tokenContractIndex.toLong(), 0),
            byteArrayOf(),
        )
        return SerializationUtils.serializeTransfers(listOf(transfer))
    }
}
