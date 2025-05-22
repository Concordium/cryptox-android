package com.concordium.wallet.ui.bakerdelegation.common

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.sdk.crypto.bls.BLSPublicKey
import com.concordium.sdk.crypto.bls.BLSSecretKey
import com.concordium.sdk.crypto.ed25519.ED25519PublicKey
import com.concordium.sdk.crypto.ed25519.ED25519SecretKey
import com.concordium.sdk.crypto.vrf.VRFPublicKey
import com.concordium.sdk.crypto.vrf.VRFSecretKey
import com.concordium.sdk.responses.BakerId
import com.concordium.sdk.responses.transactionstatus.OpenStatus
import com.concordium.sdk.responses.transactionstatus.PartsPerHundredThousand
import com.concordium.sdk.transactions.AccountTransaction
import com.concordium.sdk.transactions.CCDAmount
import com.concordium.sdk.transactions.ConfigureBakerKeysPayload
import com.concordium.sdk.transactions.ConfigureBakerPayload
import com.concordium.sdk.transactions.ConfigureBakerTransaction
import com.concordium.sdk.transactions.ConfigureDelegationPayload
import com.concordium.sdk.transactions.ConfigureDelegationTransaction
import com.concordium.sdk.transactions.Expiry
import com.concordium.sdk.transactions.SignerEntry
import com.concordium.sdk.transactions.TransactionFactory
import com.concordium.sdk.transactions.TransactionSigner
import com.concordium.sdk.types.AccountAddress
import com.concordium.sdk.types.Nonce
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.backend.ErrorParser
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.price.TokenPriceRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.CONFIGURE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_DELEGATION
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_DELEGATION
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountNonce
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.model.BakerKeys
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.model.BakerPoolStatus
import com.concordium.wallet.data.model.DelegationTarget
import com.concordium.wallet.data.model.Transaction
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Transfer
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.data.util.FileUtil
import com.concordium.wallet.data.util.toTransaction
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigInteger
import java.util.Date
import kotlin.math.roundToInt

class DelegationBakerViewModel(application: Application) : AndroidViewModel(application),
    KoinComponent {

    lateinit var bakerDelegationData: BakerDelegationData
    private val proxyRepository = ProxyRepository()
    private val transferRepository =
        TransferRepository(App.appCore.session.walletStorage.database.transferDao())

    private var bakerPoolRequest: BackendRequest<BakerPoolStatus>? = null
    private var accountNonceRequest: BackendRequest<AccountNonce>? = null
    private val tokenPriceRepository by inject<TokenPriceRepository>()

    companion object {
        const val FILE_NAME_BAKER_KEYS = "validator-credentials.json"
        const val EXTRA_DELEGATION_BAKER_DATA = "EXTRA_DELEGATION_BAKER_DATA"
        const val AMOUNT_TOO_LARGE_FOR_POOL = -100
        const val AMOUNT_TOO_LARGE_FOR_POOL_COOLDOWN = -200
    }

    private val _transactionSuccessLiveData = MutableLiveData<Boolean>()
    val transactionSuccessLiveData: LiveData<Boolean>
        get() = _transactionSuccessLiveData

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _chainParametersLoadedLiveData = MutableLiveData<Boolean>()
    val chainParametersLoadedLiveData: LiveData<Boolean>
        get() = _chainParametersLoadedLiveData

    private val _chainParametersPassiveDelegationBakerPoolLoaded = MutableLiveData<Boolean>()
    val chainParametersPassiveDelegationBakerPoolLoaded: LiveData<Boolean>
        get() = _chainParametersPassiveDelegationBakerPoolLoaded

    private val _showDetailedLiveData = MutableLiveData<Event<Boolean>>()
    val showDetailedLiveData: LiveData<Event<Boolean>>
        get() = _showDetailedLiveData

    private val _transactionFeeLiveData = MutableLiveData<Pair<BigInteger?, Int?>>()
    val transactionFeeLiveData: LiveData<Pair<BigInteger?, Int?>>
        get() = _transactionFeeLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _bakerKeysLiveData = MutableLiveData<BakerKeys?>()
    val bakerKeysLiveData: LiveData<BakerKeys?>
        get() = _bakerKeysLiveData

    private val _fileSavedLiveData = MutableLiveData<Event<Int>>()
    val fileSavedLiveData: LiveData<Event<Int>>
        get() = _fileSavedLiveData

    private val _bakerPoolStatusLiveData = MutableLiveData<BakerPoolStatus?>()
    val bakerPoolStatusLiveData: LiveData<BakerPoolStatus?>
        get() = _bakerPoolStatusLiveData

    private val _eurRateLiveData = MutableLiveData<String?>()
    val eurRateLiveData: LiveData<String?>
        get() = _eurRateLiveData

    private val _transaction = MutableLiveData<Transaction?>()
    val transaction: LiveData<Transaction?>
        get() = _transaction

    fun initialize(bakerDelegationData: BakerDelegationData) {
        this.bakerDelegationData = bakerDelegationData
    }

    fun restakeHasChanged(): Boolean {
        return bakerDelegationData.restake != bakerDelegationData.oldRestake
    }

    fun stakedAmountHasChanged(): Boolean {
        return bakerDelegationData.amount != bakerDelegationData.oldStakedAmount
    }

    fun metadataUrlHasChanged(): Boolean {
        return if (bakerDelegationData.type == REGISTER_BAKER) (bakerDelegationData.metadataUrl?.length
            ?: 0) > 0
        else bakerDelegationData.metadataUrl != bakerDelegationData.oldMetadataUrl
    }

    fun commissionRatesHasChanged(): Boolean =
        (bakerDelegationData.type == REGISTER_BAKER || bakerDelegationData.type == CONFIGURE_BAKER || bakerDelegationData.type == UPDATE_BAKER_POOL)
                && (bakerDelegationData.oldCommissionRates?.bakingCommission != bakerDelegationData.bakingCommissionRate
                || bakerDelegationData.oldCommissionRates?.transactionCommission != bakerDelegationData.transactionCommissionRate)

    fun openStatusHasChanged(): Boolean {
        return bakerDelegationData.bakerPoolInfo?.openStatus != bakerDelegationData.oldOpenStatus
    }

    fun poolHasChanged(): Boolean {
        if (bakerDelegationData.isLPool && bakerDelegationData.oldDelegationIsBaker)
            return true
        if (bakerDelegationData.isBakerPool && !bakerDelegationData.oldDelegationIsBaker)
            return true
        if (bakerDelegationData.isBakerPool && bakerDelegationData.poolId != (bakerDelegationData.oldDelegationTargetPoolId?.toString()
                ?: "")
        )
            return true
        return false
    }

    fun isBakerPool(): Boolean {
        return bakerDelegationData.account.delegation?.delegationTarget?.delegateType == DelegationTarget.TYPE_DELEGATE_TO_BAKER
    }

    fun isLPool(): Boolean {
        return bakerDelegationData.account.delegation?.delegationTarget?.delegateType == DelegationTarget.TYPE_DELEGATE_TO_L_POOL
    }

    fun isOpenBaker(): Boolean {
        return bakerDelegationData.bakerPoolInfo?.openStatus == BakerPoolInfo.OPEN_STATUS_OPEN_FOR_ALL
    }

    fun isUpdatingDelegation(): Boolean {
        bakerDelegationData.account.delegation?.let { return it.stakedAmount != BigInteger.ZERO }
        return false
    }

    fun isLoweringDelegation(): Boolean {
        bakerDelegationData.amount?.let { amount ->
            if (amount < (bakerDelegationData.oldStakedAmount ?: BigInteger.ZERO))
                return true
        }
        return false
    }

    fun isInCoolDown(): Boolean {
        return bakerDelegationData.account.delegation?.pendingChange != null || bakerDelegationData.account.baker?.pendingChange != null
    }

    fun isBakerSuspended(): Boolean {
        return bakerDelegationData.account.isBakerSuspended
                || bakerDelegationData.account.isDelegationBakerSuspended
    }

    fun isBakerPrimedForSuspension(): Boolean {
        return bakerDelegationData.account.isBakerPrimedForSuspension
    }

    fun isBakerSuspendable(): Boolean {
        return bakerDelegationData.account.baker?.isSuspended != true
    }

    fun isBakerResumable(): Boolean {
        return bakerDelegationData.account.baker?.isSuspended == true
    }

    fun isInitialSetup(): Boolean {
        return (!bakerDelegationData.isBakerPool && !bakerDelegationData.isLPool)
    }

    fun selectBakerPool() {
        this.bakerDelegationData.isLPool = false
        this.bakerDelegationData.isBakerPool = true
    }

    fun selectLPool() {
        this.bakerDelegationData.isLPool = true
        this.bakerDelegationData.isBakerPool = false
        this.bakerDelegationData.poolId = ""
    }

    fun selectOpenStatus(openStatus: BakerPoolInfo) {
        bakerDelegationData.bakerPoolInfo = openStatus
    }

    fun markRestake(restake: Boolean) {
        this.bakerDelegationData.restake = restake
        loadTransactionFee(true)
    }

    fun setPoolID(id: String) {
        bakerDelegationData.poolId = id
    }

    fun getPoolId(): String {
        return bakerDelegationData.poolId
    }

    fun getStakeInputMax(): BigInteger? {
        var max: BigInteger? = null
        val allPoolTotalCapital = bakerDelegationData.passiveDelegation?.allPoolTotalCapital
        val capitalBound: BigInteger =
            (bakerDelegationData.chainParameters?.capitalBound?.times(100))
                ?.toBigDecimal()
                ?.toBigInteger()
                ?: BigInteger.ZERO
        if (allPoolTotalCapital != null) {
            max = allPoolTotalCapital * capitalBound
            if (bakerDelegationData.type != REGISTER_BAKER) {
                bakerDelegationData.bakerPoolStatus?.delegatedCapital?.let { delegatedCapital ->
                    max -= delegatedCapital
                }
            }
        }
        return max?.div(BigInteger.valueOf(100))
    }

    fun validatePoolId() {
        if (bakerDelegationData.isLPool) {
            bakerDelegationData.bakerPoolStatus = null
            _showDetailedLiveData.value = Event(true)
        } else {
            _waitingLiveData.value = true
            bakerPoolRequest?.dispose()
            bakerPoolRequest = proxyRepository.getBakerPool(getPoolId(),
                {
                    bakerDelegationData.bakerPoolStatus = it
                    _waitingLiveData.value = false
                    val stakedAmount: Long =
                        bakerDelegationData.account.delegation?.stakedAmount?.toLong() ?: 0
                    val delegatedCapital: Long =
                        bakerDelegationData.bakerPoolStatus?.delegatedCapital?.toLong() ?: 0
                    val delegatedCapitalCap: Long =
                        bakerDelegationData.bakerPoolStatus?.delegatedCapitalCap?.toLong() ?: 0
                    val openStatus = bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus
                    val changePool =
                        (bakerDelegationData.oldDelegationTargetPoolId ?: 0) != getPoolId().toLong()
                    if (bakerDelegationData.type == UPDATE_DELEGATION && openStatus == BakerPoolInfo.OPEN_STATUS_CLOSED_FOR_ALL)
                        _errorLiveData.value =
                            Event(R.string.delegation_register_delegation_pool_id_closed)
                    else if ((bakerDelegationData.type == REGISTER_DELEGATION ||
                                bakerDelegationData.type == UPDATE_DELEGATION) &&
                        (openStatus == BakerPoolInfo.OPEN_STATUS_CLOSED_FOR_NEW || openStatus == BakerPoolInfo.OPEN_STATUS_CLOSED_FOR_ALL))
                        _errorLiveData.value =
                            Event(R.string.delegation_register_delegation_pool_id_closed)
                    else if (changePool && !isInCoolDown() && stakedAmount + delegatedCapital > delegatedCapitalCap)
                        _errorLiveData.value = Event(AMOUNT_TOO_LARGE_FOR_POOL)
                    else if (changePool && isInCoolDown() && stakedAmount + delegatedCapital > delegatedCapitalCap)
                        _errorLiveData.value = Event(AMOUNT_TOO_LARGE_FOR_POOL_COOLDOWN)
                    else
                        _showDetailedLiveData.value = Event(true)
                    _waitingLiveData.value = false
                },
                {
                    _waitingLiveData.value = false
                    _errorLiveData.value =
                        Event(R.string.delegation_register_delegation_pool_id_error)
                }
            )
        }
    }

    fun getBakerPool(bakerId: String) {
        proxyRepository.getBakerPool(bakerId,
            {
                _bakerPoolStatusLiveData.value = it
            }, {
                _bakerPoolStatusLiveData.value = null
            }
        )
    }

    fun loadTransactionFee(
        notifyObservers: Boolean,
        requestId: Int? = null,
        metadataSizeForced: Int? = null,
    ) {
        val suspended: Boolean? =
            if (bakerDelegationData.type == CONFIGURE_BAKER)
                bakerDelegationData.toSetBakerSuspended
            else
                null

        val amount =
            if (suspended != null)
                null
            else if (bakerDelegationData.type in setOf(
                    UPDATE_DELEGATION,
                    UPDATE_BAKER_STAKE,
                    CONFIGURE_BAKER
                )
            )
                bakerDelegationData.amount
            else
                null

        val restake = when (bakerDelegationData.type) {
            UPDATE_DELEGATION, UPDATE_BAKER_STAKE, CONFIGURE_BAKER -> if (restakeHasChanged()) bakerDelegationData.restake else null
            else -> null
        }

        val targetChange: Boolean? =
            if (bakerDelegationData.type == UPDATE_DELEGATION && poolHasChanged()) true else null

        val metadataSize = metadataSizeForced ?: when (bakerDelegationData.type) {
            REGISTER_BAKER -> {
                null
            }

            UPDATE_BAKER_POOL, CONFIGURE_BAKER -> {
                if (metadataUrlHasChanged() || (openStatusHasChanged() &&
                            bakerDelegationData.bakerPoolInfo?.openStatus == BakerPoolInfo.OPEN_STATUS_OPEN_FOR_ALL)
                ) {
                    (bakerDelegationData.metadataUrl?.length ?: 0)
                } else
                    null
            }

            else -> null
        }

        val openStatus: String? =
            if ((bakerDelegationData.type == UPDATE_BAKER_POOL || bakerDelegationData.type == CONFIGURE_BAKER) && openStatusHasChanged()) {
                bakerDelegationData.bakerPoolInfo?.openStatus
            } else null

        proxyRepository.getTransferCost(
            type = bakerDelegationData.type,
            amount = amount,
            restake = restake,
            lPool = bakerDelegationData.isLPool,
            targetChange = targetChange,
            metadataSize = metadataSize,
            openStatus = openStatus,
            suspended = suspended,
            success = {
                bakerDelegationData.energy = it.energy
                bakerDelegationData.cost = it.cost
                if (notifyObservers)
                    _transactionFeeLiveData.value = Pair(bakerDelegationData.cost, requestId)
            },
            failure = {
                handleBackendError(it)
            }
        )
    }

    fun loadChainParameters() = viewModelScope.launch {
        try {
            bakerDelegationData.chainParameters = proxyRepository.getChainParameters()
            _chainParametersLoadedLiveData.value = true
        } catch (e: Exception) {
            _chainParametersLoadedLiveData.value = false
            handleBackendError(e)
        }
    }

    fun loadChainParametersPassiveDelegationAndPossibleBakerPool() {
        runBlocking {
            val tasks = mutableListOf(
                async(Dispatchers.IO) {
                    val response = proxyRepository.getPassiveDelegationSuspended()
                    if (response.isSuccessful) {
                        response.body()?.let {
                            bakerDelegationData.passiveDelegation = it
                        }
                    } else {
                        val error = ErrorParser.parseError(response)
                        _errorLiveData.value = error?.let { Event(it.error) }
                    }
                },
                async(Dispatchers.IO) {
                    try {
                        bakerDelegationData.chainParameters = proxyRepository.getChainParameters()
                    } catch (e: Exception) {
                        handleBackendError(e)
                    }
                }
            )
            if (bakerDelegationData.type != REGISTER_BAKER) {
                tasks.add(async(Dispatchers.IO) {
                    val response =
                        proxyRepository.getBakerPoolSuspended(bakerDelegationData.account.baker?.bakerId.toString())
                    if (response.isSuccessful) {
                        response.body()?.let {
                            bakerDelegationData.bakerPoolStatus = it
                        }
                    } else {
                        val error = ErrorParser.parseError(response)
                        _errorLiveData.value = error?.let { Event(it.error) }
                    }
                })
            }
            tasks.awaitAll()
            _chainParametersPassiveDelegationBakerPoolLoaded.value = true
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    fun prepareTransaction() {
        if (bakerDelegationData.amount == null
            && bakerDelegationData.toSetBakerSuspended == null
            && bakerDelegationData.type != UPDATE_BAKER_KEYS
            && bakerDelegationData.type != UPDATE_BAKER_POOL
            || bakerDelegationData.toSetBakerSuspended != null
            && bakerDelegationData.type != CONFIGURE_BAKER
        ) {
            _errorLiveData.value = Event(R.string.app_error_general)
            return
        }
        getAccountNonce()
    }

    private fun getAccountNonce() {
        _waitingLiveData.value = true
        accountNonceRequest?.dispose()
        accountNonceRequest = bakerDelegationData.account.let { account ->
            proxyRepository.getAccountNonce(account.address,
                { accountNonce ->
                    bakerDelegationData.accountNonce = accountNonce
                    _showAuthenticationLiveData.value = Event(true)
                    _waitingLiveData.value = false
                },
                {
                    _waitingLiveData.value = false
                    handleBackendError(it)
                }
            )
        }
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        _waitingLiveData.value = true
        decryptAndContinue(password)
    }

    private suspend fun decryptAndContinue(password: String) {
        // Decrypt the private data
        Log.d("decryptAndContinue")
        bakerDelegationData.account.let { account ->
            val storageAccountDataEncrypted = account.encryptedAccountData
            if (storageAccountDataEncrypted == null) {
                _errorLiveData.value = Event(R.string.app_error_general)
                _waitingLiveData.value = false
                return
            }
            val decryptedJson = App.appCore.auth
                .decrypt(
                    password = password,
                    encryptedData = storageAccountDataEncrypted,
                )
                ?.let(::String)

            if (decryptedJson != null) {
                val credentialsOutput =
                    App.appCore.gson.fromJson(decryptedJson, StorageAccountData::class.java)
                if (bakerDelegationData.isBakerFlow())
                    createBakingTransaction(credentialsOutput.accountKeys.getSignerEntry())
                else
                    createDelegationTransaction(credentialsOutput.accountKeys.getSignerEntry())
            } else {
                _errorLiveData.value = Event(R.string.app_error_encryption)
                _waitingLiveData.value = false
            }
        }
    }

    private suspend fun createBakingTransaction(signer: SignerEntry) {
        val from = bakerDelegationData.account.address
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        val energy = bakerDelegationData.energy
        val nonce = bakerDelegationData.accountNonce

        if (nonce == null || energy == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }

        val capital: String? = bakerDelegationData.amount
            ?.toString()
            ?.takeIf {
                bakerDelegationData.type != UPDATE_BAKER_KEYS &&
                        bakerDelegationData.type != UPDATE_BAKER_POOL && stakedAmountHasChanged()
            }

        val restakeEarnings: Boolean? = bakerDelegationData.restake
            .takeIf {
                bakerDelegationData.type != UPDATE_BAKER_KEYS &&
                        bakerDelegationData.type != UPDATE_BAKER_POOL &&
                        bakerDelegationData.type != REMOVE_BAKER && restakeHasChanged()
            }

        val metadataUrl = (bakerDelegationData.metadataUrl ?: "")
            .takeIf {
                bakerDelegationData.type == REGISTER_BAKER ||
                        bakerDelegationData.type == UPDATE_BAKER_POOL && metadataUrlHasChanged()
            }

        val openStatus =
            if (
                bakerDelegationData.type == UPDATE_BAKER_KEYS ||
                bakerDelegationData.type == REMOVE_BAKER ||
                bakerDelegationData.type == UPDATE_BAKER_STAKE
            )
                null
            else if (openStatusHasChanged())
                bakerDelegationData.bakerPoolInfo?.openStatus
            else
                null

        val bakerKeys = bakerDelegationData.bakerKeys
            .takeUnless { bakerDelegationData.type == REMOVE_BAKER }

        val transactionFeeCommission = bakerDelegationData.transactionCommissionRate
            .takeIf { commissionRatesHasChanged() }

        val bakingRewardCommission = bakerDelegationData.bakingCommissionRate
            .takeIf { commissionRatesHasChanged() }

        val finalizationRewardCommission = bakerDelegationData.finalizationCommissionRate
            .takeIf { commissionRatesHasChanged() }

        val suspended = bakerDelegationData.toSetBakerSuspended
            ?.takeIf { bakerDelegationData.type == CONFIGURE_BAKER }

        val transaction: ConfigureBakerTransaction = try {
            val configureBakerPayload = ConfigureBakerPayload
                .builder()
                .capital(capital?.let(CCDAmount::fromMicro))
                .restakeEarnings(restakeEarnings)
                .metadataUrl(metadataUrl)
                .openForDelegation(
                    when (openStatus) {
                        BakerPoolInfo.OPEN_STATUS_OPEN_FOR_ALL ->
                            OpenStatus.OPEN_FOR_ALL

                        BakerPoolInfo.OPEN_STATUS_CLOSED_FOR_NEW ->
                            OpenStatus.CLOSED_FOR_NEW

                        BakerPoolInfo.OPEN_STATUS_CLOSED_FOR_ALL ->
                            OpenStatus.CLOSED_FOR_ALL

                        else ->
                            null
                    }
                )
                .keysWithProofs(bakerKeys?.run {
                    ConfigureBakerKeysPayload.getNewConfigureBakerKeysPayload(
                        AccountAddress.from(from),
                        com.concordium.sdk.crypto.bakertransactions.BakerKeys
                            .builder()
                            .signatureSignKey(ED25519SecretKey.from(signatureSignKey))
                            .signatureVerifyKey(ED25519PublicKey.from(signatureVerifyKey))
                            .electionPrivateKey(VRFSecretKey.from(electionPrivateKey))
                            .electionVerifyKey(VRFPublicKey.from(electionVerifyKey))
                            .aggregationSignKey(BLSSecretKey.from(aggregationSignKey))
                            .aggregationVerifyKey(BLSPublicKey.from(aggregationVerifyKey))
                            .build()
                    )
                })
                .transactionFeeCommission(transactionFeeCommission?.let {
                    PartsPerHundredThousand.from((it * 100000).roundToInt())
                })
                .bakingRewardCommission(bakingRewardCommission?.let {
                    PartsPerHundredThousand.from((it * 100000).roundToInt())
                })
                .finalizationRewardCommission(finalizationRewardCommission?.let {
                    PartsPerHundredThousand.from((it * 100000).roundToInt())
                })
                .suspended(suspended)
                .build()

            TransactionFactory.newConfigureBaker()
                .sender(AccountAddress.from(from))
                .signer(TransactionSigner.from(signer))
                .nonce(Nonce.from(nonce.nonce.toLong()))
                .expiry(Expiry.from(expiry))
                .payload(configureBakerPayload)
                .build()
        } catch (e: Exception) {
            Log.e("Error creating transaction", e)
            _errorLiveData.value = Event(R.string.app_error_lib)
            _waitingLiveData.value = false
            return
        }

        // Do not disable waiting state yet
        submitConfigurationTransaction(
            transaction = transaction,
            localTransactionType = TransactionType.LOCAL_BAKER,
        )
    }

    private suspend fun createDelegationTransaction(signer: SignerEntry) {
        val from = bakerDelegationData.account.address
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000
        val energy = bakerDelegationData.energy
        val nonce = bakerDelegationData.accountNonce

        if (nonce == null || energy == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }

        val capital: String? = bakerDelegationData.amount
            ?.toString()
            ?.takeIf { stakedAmountHasChanged() }

        val restakeEarnings: Boolean? = bakerDelegationData.restake
            .takeIf { bakerDelegationData.type != REMOVE_DELEGATION && restakeHasChanged() }

        val delegationTarget: com.concordium.sdk.responses.transactionstatus.DelegationTarget? =
            if (bakerDelegationData.type == REGISTER_DELEGATION
                || bakerDelegationData.type == UPDATE_DELEGATION && poolHasChanged()
            ) {
                if (bakerDelegationData.isBakerPool && bakerDelegationData.poolId.isNotEmpty())
                    com.concordium.sdk.responses.transactionstatus.DelegationTarget
                        .newBakerDelegationTarget(BakerId.from(bakerDelegationData.poolId.toLong()))
                else
                    com.concordium.sdk.responses.transactionstatus.DelegationTarget
                        .newPassiveDelegationTarget()
            } else {
                null
            }

        val transaction: ConfigureDelegationTransaction = try {
            TransactionFactory
                .newConfigureDelegation()
                .sender(AccountAddress.from(from))
                .signer(TransactionSigner.from(signer))
                .nonce(Nonce.from(nonce.nonce.toLong()))
                .expiry(Expiry.from(expiry))
                .payload(
                    ConfigureDelegationPayload
                        .builder()
                        .capital(capital?.let(CCDAmount::fromMicro))
                        .restakeEarnings(restakeEarnings)
                        .delegationTarget(delegationTarget)
                        .build()
                )
                .build()
        } catch (e: Exception) {
            Log.e("Error creating transaction", e)
            _errorLiveData.value = Event(R.string.app_error_lib)
            _waitingLiveData.value = false
            return
        }

        // Do not disable waiting state yet
        submitConfigurationTransaction(
            transaction = transaction,
            localTransactionType = TransactionType.LOCAL_DELEGATION,
        )
    }

    private suspend fun submitConfigurationTransaction(
        transaction: AccountTransaction,
        localTransactionType: TransactionType,
    ) = withContext(Dispatchers.IO) {

        Log.d("transaction: $transaction")

        val submissionId: String = try {
            proxyRepository
                .submitSdkTransaction(transaction)
                .submissionId
        } catch (e: Exception) {
            Log.e("Error submitting transaction", e)
            withContext(Dispatchers.Main) {
                _waitingLiveData.value = false
                handleBackendError(e)
            }
            return@withContext
        }

        Log.d("Transaction submitted: $submissionId")

        bakerDelegationData.submissionId = submissionId

        val submissionStatus = try {
            proxyRepository
                .getSubmissionStatus(submissionId)
                .status
        } catch (e: Exception) {
            Log.e("Error checking submission status", e)
            withContext(Dispatchers.Main) {
                _waitingLiveData.value = false
                _errorLiveData.value = Event(BackendErrorHandler.getExceptionStringRes(e))
            }
            return@withContext
        }

        // Do not disable waiting state yet
        finishTransferCreation(
            submissionId = submissionId,
            submissionStatus = submissionStatus,
            localTransactionType = localTransactionType,
        )
    }

    private suspend fun finishTransferCreation(
        submissionId: String,
        submissionStatus: TransactionStatus,
        localTransactionType: TransactionType,
    ) = withContext(Dispatchers.Main) {
        val createdAt = Date().time

        val accountId = bakerDelegationData.account.id
        val fromAddress = bakerDelegationData.account.address
        val cost = bakerDelegationData.cost
        val expiry = (DateTimeUtil.nowPlusMinutes(10).time) / 1000

        if (cost == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return@withContext
        }

        val transfer = Transfer(
            0,
            accountId,
            cost,
            BigInteger.ZERO,
            fromAddress,
            fromAddress,
            expiry,
            "",
            createdAt,
            submissionId,
            submissionStatus,
            TransactionOutcome.UNKNOWN,
            localTransactionType,
            //but amount is negative so it is listed as incoming positive
            null,
            0,
            null
        )

        transferRepository.insert(transfer)
        _transaction.postValue(transfer.toTransaction())
        _waitingLiveData.value = false
        _transactionSuccessLiveData.value = true
    }

    fun generateKeys() {
        viewModelScope.launch {
            val bakerKeys = App.appCore.cryptoLibrary.generateBakerKeys()
            if (bakerKeys == null) {
                _errorLiveData.value = Event(R.string.app_error_lib)
            } else {
                bakerDelegationData.bakerKeys = bakerKeys
                _bakerKeysLiveData.value = bakerKeys
            }
        }
    }

    fun saveFileToLocalFolder(destinationUri: Uri) {
        bakerKeysJson()?.let { bakerKeysJson ->
            viewModelScope.launch {
                FileUtil.writeFile(destinationUri, FILE_NAME_BAKER_KEYS, bakerKeysJson)
                _fileSavedLiveData.value = Event(R.string.baker_keys_saved_local)
            }
        }
    }

    private fun bakerKeysJson(): String? {
        _bakerKeysLiveData.value?.let { bakerKeys ->
            bakerKeys.bakerId = bakerDelegationData.account.index
            return if (bakerKeys.toString().isNotEmpty())
                App.appCore.gson.toJson(bakerKeys)
            else
                null
        }
        return null
    }

    fun getAvailableBalance(): BigInteger = bakerDelegationData.account.balance

    fun setSelectedCommissionRates(
        transactionRate: Double?,
        bakingRate: Double?,
    ) {
        bakerDelegationData.transactionCommissionRate = transactionRate
        bakerDelegationData.bakingCommissionRate = bakingRate
    }

    fun getMaxDelegationBalance(): BigInteger =
        bakerDelegationData.account.balance - (bakerDelegationData.cost ?: BigInteger.ZERO)

    fun loadEURRate(amount: BigInteger) = viewModelScope.launch {
        tokenPriceRepository.getEurPerMicroCcd()
            .onSuccess { eurPerMicroCcd ->
                _eurRateLiveData.postValue(
                    CurrencyUtil.toEURRate(
                        amount,
                        eurPerMicroCcd,
                    )
                )
            }
            .onFailure(::handleBackendError)
    }
}
