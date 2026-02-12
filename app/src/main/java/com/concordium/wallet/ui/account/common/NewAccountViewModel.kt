package com.concordium.wallet.ui.account.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateCredentialInputV1
import com.concordium.wallet.data.cryptolib.CreateCredentialOutput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.SubmissionStatusResponse
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.RandomUtil
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class NewAccountViewModel(application: Application) :
    AndroidViewModel(application) {

    private val identityProviderRepository = IdentityProviderRepository()
    private val proxyRepository = ProxyRepository()
    private val accountRepository: AccountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    private val recipientRepository: RecipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())
    private val gson = App.appCore.gson

    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var submitCredentialRequest: BackendRequest<SubmissionData>? = null
    private var accountSubmissionStatusRequest: BackendRequest<SubmissionStatusResponse>? = null
    private var firstAccount: Boolean = false

    private var tempData = TempData()
    lateinit var accountName: String
    lateinit var identity: Identity

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _gotoAccountCreatedLiveData = MutableLiveData<Event<Account>>()
    val gotoAccountCreatedLiveData: LiveData<Event<Account>>
        get() = _gotoAccountCreatedLiveData

    private val _gotoFailedLiveData = MutableLiveData<Event<Pair<Boolean, BackendError?>>>()
    val gotoFailedLiveData: LiveData<Event<Pair<Boolean, BackendError?>>>
        get() = _gotoFailedLiveData

    open fun initialize(accountName: String, identity: Identity, firstAccount: Boolean = false) {
        this.accountName = accountName
        this.identity = identity
        this.firstAccount = firstAccount
    }

    class TempData {
        var globalParams: GlobalParams? = null
        var submissionId: String? = null
        var accountAddress: String? = null
        var encryptedAccountData: EncryptedData? = null
        var credential: CredentialWrapper? = null
        var nextCredNumber: Int? = null
    }

    override fun onCleared() {
        super.onCleared()
        globalParamsRequest?.dispose()
        submitCredentialRequest?.dispose()
        accountSubmissionStatusRequest?.dispose()
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        if (throwable is BackendErrorException) {
            _gotoFailedLiveData.postValue(Event(Pair(true, throwable.error)))
        } else {
            _errorLiveData.postValue(Event(BackendErrorHandler.getExceptionStringRes(throwable)))
        }
    }

    fun createAccount() {
        getGlobalInfo()
    }

    private fun getGlobalInfo() {
        // Show waiting state for the full flow, but remove it of any errors occur
        _waitingLiveData.postValue(true)
        globalParamsRequest?.dispose()
        globalParamsRequest = identityProviderRepository.getIGlobalInfo(
            success = {
                App.appCore.session.walletStorage.setupPreferences.setHasCompletedOnboarding(true)
                tempData.globalParams = it.value
                _showAuthenticationLiveData.postValue(Event(true))
                _waitingLiveData.postValue(false)
            },
            failure = {
                _waitingLiveData.postValue(false)
                handleBackendError(it)
            }
        )
    }

    fun continueWithPassword(password: String) = viewModelScope.launch(Dispatchers.IO) {
        _waitingLiveData.postValue(true)
        decryptAndContinue(password)
    }

    private suspend fun decryptAndContinue(password: String) {
        val globalParams = tempData.globalParams

        // Proceed to credentials creation as there is nothing to decrypt.
        if (globalParams == null) {
            _errorLiveData.value = Event(R.string.app_error_general)
            _waitingLiveData.value = false
            return
        }

        createCredentials(password, globalParams)
    }

    private suspend fun createCredentials(
        password: String,
        globalParams: GlobalParams,
    ) {
        val identityProvider = identity.identityProvider
        val identityObject = identity.identityObject
        val idProviderInfo = identityProvider.ipInfo
        val arsInfos = identityProvider.arsInfos

        if (identityObject == null) {
            Log.e("Identity is not ready to use for account creation (no identityObject")
            _errorLiveData.postValue(Event(R.string.app_error_general))
            _waitingLiveData.postValue(false)
            return
        }

        tempData.nextCredNumber = accountRepository.nextCredNumber(identity.id)

        val net = App.appCore.session.network.hdWalletNetwork
        val seed = App.appCore.session.walletStorage.setupPreferences.getSeedHex(password)

        val credentialInput = CreateCredentialInputV1(
            ipInfo = idProviderInfo,
            arsInfos = arsInfos,
            global = globalParams,
            identityObject = identityObject,
            revealedAttributes = JsonArray(),
            seed = seed,
            net = net.value,
            identityIndex = identity.identityIndex,
            accountNumber = tempData.nextCredNumber ?: 0,
            expiry = (DateTimeUtil.nowPlusMinutes(5).time) / 1000,
        )

        val output: CreateCredentialOutput? =
            App.appCore.cryptoLibrary.createCredentialV1(credentialInput)

        if (output == null) {
            _errorLiveData.postValue(Event(R.string.app_error_lib))
            _waitingLiveData.postValue(false)
        } else {
            tempData.accountAddress = output.accountAddress
            val jsonToBeEncrypted = gson.toJson(
                StorageAccountData(
                    output.accountAddress,
                    output.accountKeys,
                    output.encryptionSecretKey,
                    output.commitmentsRandomness
                )
            )

            val storageAccountDataEncrypted = App.appCore.auth
                .encrypt(
                    password = password,
                    data = jsonToBeEncrypted.toByteArray()
                )
            if (storageAccountDataEncrypted != null) {
                tempData.encryptedAccountData = storageAccountDataEncrypted
                tempData.credential = output.credential
                submitCredential(output.credential)
            } else {
                _errorLiveData.postValue(Event(R.string.app_error_encryption))
                _waitingLiveData.postValue(false)
            }
        }
    }

    private fun submitCredential(credentialWrapper: CredentialWrapper) {
        _waitingLiveData.postValue(true)
        submitCredentialRequest?.dispose()
        submitCredentialRequest = proxyRepository.submitCredential(
            credentialWrapper,
            {
                tempData.submissionId = it.submissionId
                finishAccountCreation()
                // Do not disable waiting state yet
            },
            {
                _waitingLiveData.postValue(false)
                handleBackendError(it)
            }
        )
    }

    private fun finishAccountCreation() {
        val accountAddress = tempData.accountAddress
        val submissionId = tempData.submissionId
        val encryptedAccountData = tempData.encryptedAccountData
        val credential = tempData.credential
        val submissionStatus = TransactionStatus.RECEIVED
        if (accountAddress == null || submissionId == null || encryptedAccountData == null ||
            credential == null
        ) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            _waitingLiveData.postValue(false)
            return
        }

        val newAccount = Account(
            identityId = identity.id,
            name = accountName,
            address = accountAddress,
            submissionId = submissionId,
            transactionStatus = submissionStatus,
            encryptedAccountData = encryptedAccountData,
            credential = credential,
            credNumber = tempData.nextCredNumber ?: 0,
            isActive = firstAccount,
            iconId = RandomUtil.getRandomInt()
        )
        saveNewAccount(newAccount)
    }

    private fun saveNewAccount(account: Account) = viewModelScope.launch(Dispatchers.IO) {
        val accountId = accountRepository.insertAndActivate(account)
        account.id = accountId.toInt()
        if (firstAccount) {
            App.appCore.setup.setShowReviewDialogSnapshotTime()
        }
        // Also save a recipient representing this account
        recipientRepository.insert(Recipient(account))
        _gotoAccountCreatedLiveData.postValue(Event(account))
    }
}
