package com.concordium.wallet.ui.seed.recoverprocess

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.core.notifications.UpdateNotificationsSubscriptionUseCase
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateCredentialInputV1
import com.concordium.wallet.data.cryptolib.CreateCredentialOutput
import com.concordium.wallet.data.cryptolib.GenerateRecoveryRequestInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.IdentityObject
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.IdentityDao
import com.concordium.wallet.data.room.IdentityWithAccounts
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.ui.cis2.defaults.DefaultFungibleTokensManager
import com.concordium.wallet.ui.cis2.defaults.DefaultTokensManagerFactory
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.ui.seed.recoverprocess.retrofit.IdentityProviderApiInstance
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.RandomUtil
import com.google.gson.JsonArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Serializable
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap

data class RecoverProcessData(
    var identitiesWithAccounts: List<IdentityWithAccounts> = mutableListOf(),
    var noResponseFrom: MutableSet<String> = mutableSetOf()
) : Serializable

class RecoverProcessViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val RECOVER_PROCESS_DATA = "RECOVER_PROCESS_DATA"
        const val STATUS_DONE = 1
        private const val IDENTITY_GAP_MAX = 20
        private const val ACCOUNT_GAP_MAX = 20
    }

    private val identityRepository =
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    private val accountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    private val recipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())
    private val defaultFungibleTokensManager: DefaultFungibleTokensManager

    private val identitiesWithAccountsFound = mutableListOf<IdentityWithAccounts>()
    private var stop = false
    private var identityGaps: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
    private var accountGaps: ConcurrentHashMap<Int, Int> = ConcurrentHashMap()
    private var password = ""
    private var identityNamePrefix = ""
    private val net = AppConfig.net
    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var identityProvidersRequest: BackendRequest<ArrayList<IdentityProvider>>? = null
    private var globalInfo: GlobalParamsWrapper? = null
    private var identityProviders: ArrayList<IdentityProvider>? = null
    private val identityGapMutex = Mutex()
    private val accountGapMutex = Mutex()
    private val updateNotificationsSubscriptionUseCase by lazy(::UpdateNotificationsSubscriptionUseCase)

    val statusChanged: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val progressIdentities: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val progressAccounts: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val errorLiveData: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val recoverProcessData = RecoverProcessData()

    init {
        val defaultTokensManagerFactory = DefaultTokensManagerFactory(
            contractTokensRepository = ContractTokensRepository(
                App.appCore.session.walletStorage.database.contractTokenDao()
            ),
        )
        defaultFungibleTokensManager = defaultTokensManagerFactory.getDefaultFungibleTokensManager()
    }

    fun recoverIdentitiesAndAccounts(password: String, identityNamePrefix: String) {
        this.password = password
        this.identityNamePrefix = identityNamePrefix
        waiting.postValue(true)
        identityGaps = ConcurrentHashMap()
        getGlobalInfo()
    }

    private fun getGlobalInfo() {
        globalParamsRequest = ProxyRepository().getIGlobalInfo(
            { globalInfo ->
                this.globalInfo = globalInfo
                getIdentityProviderInfo()
            },
            {
                handleBackendError(it)
            }
        )
    }

    private fun getIdentityProviderInfo() {
        identityProvidersRequest = IdentityProviderRepository().getIdentityProviderInfo(
            { identityProviders ->
                this.identityProviders =
                    identityProviders.filterNot { it.ipInfo.ipDescription.name == "instant_fail_provider" } as ArrayList<IdentityProvider>
                getIdentities()
            },
            {
                handleBackendError(it)
            }
        )
    }

    private fun getIdentities() {
        globalInfo?.let { globalInfo ->
            identityProviders?.forEach { identityProvider ->
                identityGaps[identityProvider.ipInfo.ipDescription.url] = 0
                if (!stop) {
                    viewModelScope.launch {
                        getIdentityFromProvider(identityProvider, globalInfo, 0)
                    }
                }
            }
        }
    }

    private suspend fun getIdentityFromProvider(
        identityProvider: IdentityProvider,
        globalInfo: GlobalParamsWrapper,
        identityIndex: Int
    ) {
        if ((identityGaps[identityProvider.ipInfo.ipDescription.url]
                ?: IDENTITY_GAP_MAX) >= IDENTITY_GAP_MAX
        ) {
            checkAllDone()
            return
        }

        val recoverRequestUrl = getRecoverRequestUrl(identityProvider, globalInfo, identityIndex)
        if (recoverRequestUrl != null) {
            val recoverResponsePair = IdentityProviderApiInstance.safeRecoverCall(recoverRequestUrl)
            if (recoverResponsePair.first && recoverResponsePair.second != null && recoverResponsePair.second!!.value != null) {
                val identity = saveIdentity(
                    recoverResponsePair.second!!.value!!,
                    identityProvider,
                    identityIndex
                )
                CoroutineScope(Dispatchers.IO).launch {
                    getAccounts(identity)
                }
            } else {
                if (!recoverResponsePair.first)
                    recoverProcessData.noResponseFrom.add(identityProvider.displayName)
                increaseIdentityGap(identityProvider.ipInfo.ipDescription.url)
            }
        } else {
            increaseIdentityGap(identityProvider.ipInfo.ipDescription.url)
        }

        checkAllDone()

        if (!stop)
            getIdentityFromProvider(identityProvider, globalInfo, identityIndex + 1)
    }

    private suspend fun getAccounts(identity: Identity) {
        globalInfo?.let { globalInfo ->
            accountGapMutex.withLock {
                accountGaps[identity.id] = 0
            }
            if (!stop) {
                recoverAccount(identity, globalInfo, 0)
            }
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        errorLiveData.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    fun stop() {
        stop = true
        globalParamsRequest?.dispose()
        identityProvidersRequest?.dispose()
    }

    private suspend fun getRecoverRequestUrl(
        identityProvider: IdentityProvider,
        globalInfo: GlobalParamsWrapper,
        identityIndex: Int
    ): String? {
        val seed = App.appCore.session.walletStorage.setupPreferences.getSeedHex(password)
        val recoveryRequestInput = GenerateRecoveryRequestInput(
            identityProvider.ipInfo,
            globalInfo.value,
            seed,
            net,
            identityIndex,
            System.currentTimeMillis() / 1000
        )

        val output = App.appCore.cryptoLibrary.generateRecoveryRequest(recoveryRequestInput)

        if (output != null && !identityProvider.metadata.recoveryStart.isNullOrBlank()) {
            val encoded = Uri.encode(output)
            val urlFromIpInfo = "${identityProvider.metadata.recoveryStart}?state="
            return "$urlFromIpInfo$encoded"
        }

        return null
    }

    private suspend fun saveIdentity(
        identityObject: IdentityObject,
        identityProvider: IdentityProvider,
        identityIndex: Int
    ): Identity {
        val identity = Identity(
            0,
            IdentityDao.DEFAULT_NAME,
            IdentityStatus.DONE,
            "",
            "",
            0,
            identityProvider,
            identityObject,
            null,
            identityProvider.ipInfo.ipIdentity,
            identityIndex
        )
        val existingIdentity = identityRepository.findByProviderIdAndIndex(
            identityProvider.ipInfo.ipIdentity,
            identityIndex
        )
        if (existingIdentity == null) {
            val newIdentityId = identityRepository.insert(identity)
            identity.id = newIdentityId.toInt()
            identitiesWithAccountsFound.add(IdentityWithAccounts(identity, mutableListOf()))
            return identity
        }
        return existingIdentity
    }

    private suspend fun recoverAccount(
        identity: Identity,
        globalInfo: GlobalParamsWrapper,
        credNumber: Int
    ) {
        if ((accountGaps[identity.id] ?: ACCOUNT_GAP_MAX) >= ACCOUNT_GAP_MAX) {
            checkAllDone()
            return
        }

        var createCredentialOutput: CreateCredentialOutput? = null
        if (identity.identityObject != null) {
            val seed = App.appCore.session.walletStorage.setupPreferences.getSeedHex(password)
            val credentialInput = CreateCredentialInputV1(
                identity.identityProvider.ipInfo,
                identity.identityProvider.arsInfos,
                globalInfo.value,
                identity.identityObject!!,
                JsonArray(),
                seed,
                net,
                identity.identityIndex,
                credNumber,
                (DateTimeUtil.nowPlusMinutes(5).time) / 1000
            )
            createCredentialOutput = App.appCore.cryptoLibrary.createCredentialV1(credentialInput)
        }

        if (createCredentialOutput == null)
            return

        val jsonToBeEncrypted = App.appCore.gson.toJson(
            StorageAccountData(
                accountAddress = createCredentialOutput.accountAddress,
                accountKeys = createCredentialOutput.accountKeys,
                encryptionSecretKey = createCredentialOutput.encryptionSecretKey,
                commitmentsRandomness = createCredentialOutput.commitmentsRandomness
            )
        )
        val encryptedAccountData = App.appCore.auth
            .encrypt(
                password = password,
                data = jsonToBeEncrypted.toByteArray(),
            )
            ?: return

        try {
            val accountBalance =
                ProxyRepository().getAccountBalanceSuspended(createCredentialOutput.accountAddress)
            if (accountBalance.finalizedBalance != null) {
                val account = Account(
                    identityId = identity.id,
                    name = Account.getDefaultName(createCredentialOutput.accountAddress),
                    address = createCredentialOutput.accountAddress,
                    submissionId = "",
                    transactionStatus = TransactionStatus.FINALIZED,
                    encryptedAccountData = encryptedAccountData,
                    credential = createCredentialOutput.credential,
                    balance = accountBalance.finalizedBalance.accountAmount,
                    balanceAtDisposal = accountBalance.finalizedBalance.accountAtDisposal,
                    encryptedBalance = accountBalance.finalizedBalance.accountEncryptedAmount,
                    encryptedBalanceStatus =
                    if (accountBalance.finalizedBalance.accountEncryptedAmount.isDefaultEmpty())
                        ShieldedAccountEncryptionStatus.DECRYPTED
                    else
                        ShieldedAccountEncryptionStatus.ENCRYPTED,
                    releaseSchedule = accountBalance.finalizedBalance.accountReleaseSchedule,
                    cooldowns = accountBalance.finalizedBalance.accountCooldowns,
                    delegation = accountBalance.finalizedBalance.accountDelegation,
                    baker = accountBalance.finalizedBalance.accountBaker,
                    index = accountBalance.finalizedBalance.accountIndex,
                    credNumber = credNumber,
                    iconId = RandomUtil.getRandomInt()
                )

                if (accountRepository.findByAddress(account.address) == null) {
                    accountRepository.insert(account)
                    if (recipientRepository.getRecipientByAddress(account.address) == null) {
                        recipientRepository.insert(Recipient(account))
                    }
                    defaultFungibleTokensManager.addForAccount(account.address)
                    updateNotificationsSubscriptionUseCase()

                    val iWithAFound =
                        identitiesWithAccountsFound.firstOrNull { it.identity.identityProviderId == identity.identityProviderId && it.identity.identityIndex == identity.identityIndex }
                    if (iWithAFound != null)
                        iWithAFound.accounts.add(account)
                    else
                        identitiesWithAccountsFound.add(
                            IdentityWithAccounts(
                                identity,
                                mutableListOf(account)
                            )
                        )
                }
            } else {
                increaseAccountGap(identity.id)
            }
        } catch (ex: Exception) {
            stop = true
            handleBackendError(ex)
        }

        checkAllDone()

        if (!stop)
            recoverAccount(identity, globalInfo, credNumber + 1)
    }

    private fun identitiesPercent(): Int {
        if (identityGaps.size == 0) {
            progressIdentities.postValue(0)
            return 0
        }
        var identities = 0
        val gapsIterator = identityGaps.values.iterator()
        while (gapsIterator.hasNext()) {
            val gap = gapsIterator.next()
            identities += IDENTITY_GAP_MAX - gap
        }
        val total = identityGaps.size * IDENTITY_GAP_MAX
        identities = total - identities
        val percent = (identities * 100) / total
        progressIdentities.postValue(percent)
        return percent
    }

    private fun accountsPercent(): Int {
        if (accountGaps.size == 0) {
            progressAccounts.postValue(0)
            return 0
        }
        var accounts = 0
        val gapsIterator = accountGaps.values.iterator()
        while (gapsIterator.hasNext()) {
            val gap = gapsIterator.next()
            accounts += ACCOUNT_GAP_MAX - gap
        }
        val total = accountGaps.size * ACCOUNT_GAP_MAX
        accounts = total - accounts
        val percent = (accounts * 100) / total
        if ((progressAccounts.value ?: 0) < percent)
            progressAccounts.postValue(percent)
        return percent
    }

    private suspend fun checkAllDone() {
        val identitiesPercent = identitiesPercent()
        val accountsPercent = accountsPercent()
        if ((identitiesPercent >= 100 && accountsPercent >= 100) || (identitiesPercent >= 100 && accountGaps.size == 0)) {
            val identityRepository =
                IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
            val allIdentities = identityRepository.getAllNew()
            for (identity in allIdentities) {
                if (identity.name == IdentityDao.DEFAULT_NAME) {
                    identity.name = identityRepository.nextIdentityName(identityNamePrefix)
                    identityRepository.update(identity)
                    val found = identitiesWithAccountsFound.filter { it.identity.id == identity.id }
                    if (found.isNotEmpty())
                        found.first().identity.name = identity.name
                }
            }
            activateFirstAccountIfNoneActive()
            recoverProcessData.identitiesWithAccounts = identitiesWithAccountsFound
            waiting.postValue(false)
            statusChanged.postValue(STATUS_DONE)
        }
    }

    private suspend fun activateFirstAccountIfNoneActive() {
        val allAccounts = accountRepository.getAll()
        val activeAccount = accountRepository.getActive()

        if (allAccounts.isNotEmpty() && activeAccount == null) {
            accountRepository.activate(
                allAccounts
                    .sortedBy { it.id }
                    .last().address
            )

            allAccounts
                .find { it.balance > BigInteger.ZERO }
                ?.let {
                    App.appCore.setup.setHasShowReviewDialogAfterReceiveFunds(true)
                }
            App.appCore.setup.setShowReviewDialogSnapshotTime()
        }
    }

    private suspend fun increaseIdentityGap(url: String) {
        identityGapMutex.withLock {
            var plus = identityGaps[url]
            if (plus != null) {
                identityGaps[url] = ++plus
            }
        }
    }

    private suspend fun increaseAccountGap(identityId: Int) {
        accountGapMutex.withLock {
            var plus = accountGaps[identityId]
            if (plus != null) {
                accountGaps[identityId] = ++plus
            }
        }
    }
}
