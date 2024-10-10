package com.concordium.wallet.ui.account.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.AppConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.backend.BackendRequest
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.CreateCredentialInput
import com.concordium.wallet.data.cryptolib.CreateCredentialInputV1
import com.concordium.wallet.data.cryptolib.CreateCredentialOutput
import com.concordium.wallet.data.cryptolib.GenerateAccountsInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountSubmissionStatus
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.GlobalParamsWrapper
import com.concordium.wallet.data.model.PossibleAccount
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.data.model.SubmissionData
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.DateTimeUtil
import com.concordium.wallet.util.KeyCreationVersion
import com.concordium.wallet.util.Log
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class NewAccountViewModel(application: Application) :
    AndroidViewModel(application) {

    private val identityProviderRepository = IdentityProviderRepository()
    private val proxyRepository = ProxyRepository()
    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository
    private val recipientRepository: RecipientRepository
    private val gson = App.appCore.gson
    private val keyCreationVersion = KeyCreationVersion(AuthPreferences(App.appContext))

    private var globalParamsRequest: BackendRequest<GlobalParamsWrapper>? = null
    private var submitCredentialRequest: BackendRequest<SubmissionData>? = null
    private var accountSubmissionStatusRequest: BackendRequest<AccountSubmissionStatus>? = null

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

    open fun initialize(accountName: String, identity: Identity) {
        this.accountName = accountName
        this.identity = identity
    }

    class TempData {
        var globalParams: GlobalParams? = null
        var submissionId: String? = null
        var accountAddress: String? = null
        var encryptedAccountData: EncryptedData? = null
        var credential: CredentialWrapper? = null
        var nextCredNumber: Int? = null
    }

    init {
        val identityDao = WalletDatabase.getDatabase(application).identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        accountRepository = AccountRepository(accountDao)
        val recipientDao = WalletDatabase.getDatabase(application).recipientDao()
        recipientRepository = RecipientRepository(recipientDao)
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
            {
                tempData.globalParams = it.value
                _showAuthenticationLiveData.postValue(Event(true))
                _waitingLiveData.postValue(false)
            },
            {
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
        if (keyCreationVersion.useV1) {
            // Proceed to credentials creation as there is nothing to decrypt.
            if (globalParams == null) {
                _errorLiveData.value = Event(R.string.app_error_general)
                _waitingLiveData.value = false
                return
            }
            createCredentials(password, null, globalParams)
        } else {
            // Decrypt the private data.
            val privateIdObjectDataEncrypted = identity.privateIdObjectDataEncrypted!!
            if (globalParams == null) {
                _errorLiveData.postValue(Event(R.string.app_error_general))
                _waitingLiveData.postValue(false)
                return
            }
            val decryptedJson = App.appCore.authManager
                .decrypt(
                    password = password,
                    encryptedData = privateIdObjectDataEncrypted,
                )
                ?.let(::String)
            if (decryptedJson != null) {
                val privateIdObjectData = gson.fromJson(decryptedJson, RawJson::class.java)
                createCredentials(password, privateIdObjectData, globalParams)
            } else {
                _errorLiveData.postValue(Event(R.string.app_error_encryption))
                _waitingLiveData.postValue(false)
            }
        }
    }

    private suspend fun createCredentials(
        password: String,
        privateIdObjectData: RawJson?,
        globalParams: GlobalParams
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

        val output: CreateCredentialOutput? =
            if (keyCreationVersion.useV1) {
                val net = AppConfig.net
                val seed = AuthPreferences(getApplication()).getSeedHex(password)

                val credentialInput = CreateCredentialInputV1(
                    ipInfo = idProviderInfo,
                    arsInfos = arsInfos,
                    global = globalParams,
                    identityObject = identityObject,
                    revealedAttributes = JsonArray(),
                    seed = seed,
                    net = net,
                    identityIndex = identity.identityIndex,
                    accountNumber = tempData.nextCredNumber ?: 0,
                    expiry = (DateTimeUtil.nowPlusMinutes(5).time) / 1000,
                )

                App.appCore.cryptoLibrary.createCredentialV1(credentialInput)
            } else {
                requireNotNull(privateIdObjectData) {
                    "Private ID data must be set when using V0 creation"
                }

                val generateAccountsInput =
                    GenerateAccountsInput(globalParams, identityObject, privateIdObjectData)
                val nextAccountNumber = findNextAccountNumber(generateAccountsInput)
                    ?: // Error has been handled
                    return

                val credentialInput = CreateCredentialInput(
                    ipInfo = idProviderInfo,
                    arsInfos = arsInfos,
                    global = globalParams,
                    identityObject = identityObject,
                    privateIdObjectData = privateIdObjectData,
                    revealedAttributes = JsonArray(),
                    accountNumber = nextAccountNumber,
                    expiry = (DateTimeUtil.nowPlusMinutes(5).time) / 1000,
                )

                App.appCore.cryptoLibrary.createCredential(credentialInput)
            }

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

            val storageAccountDataEncrypted = App.appCore.authManager
                .encrypt(
                    password=password,
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

    private suspend fun findNextAccountNumber(generateAccountsInput: GenerateAccountsInput): Int? {
        var nextAccountNumber = identity.nextAccountNumber
        Log.d("nextAccountNumber: $nextAccountNumber")
        val maxAccounts = identity.identityObject!!.attributeList.maxAccounts

        val possibleAccountList = App.appCore.cryptoLibrary.generateAccounts(generateAccountsInput)
        if (possibleAccountList == null) {
            Log.e("Could not generate accounts, so do not allow account creation")
            _errorLiveData.postValue(Event(R.string.app_error_lib))
            _waitingLiveData.postValue(false)
            return null
        } else {
            Log.d("Generated account info for ${possibleAccountList.size} accounts")
            val next = checkExistingAccounts(possibleAccountList, nextAccountNumber)
            if (next == null) {
                _errorLiveData.postValue(Event(R.string.app_error_backend_unknown))
                _waitingLiveData.postValue(false)
                return null
            } else {
                nextAccountNumber = next
            }
        }

        Log.d("nextAccountNumber used: $nextAccountNumber")
        // Next account number starts from 0
        if (nextAccountNumber >= maxAccounts) {
            _errorLiveData.postValue(Event(R.string.new_account_identity_attributes_error_max_accounts_alt))
            _waitingLiveData.postValue(false)
            return null
        }
        identity.nextAccountNumber = nextAccountNumber + 1
        viewModelScope.launch(Dispatchers.IO) {
            identityRepository.update(identity)
        }
        return nextAccountNumber
    }

    /**
     * Returns the index for the first unused account
     */
    private suspend fun checkExistingAccounts(
        possibleAccountList: List<PossibleAccount>,
        startIndex: Int
    ): Int? {
        var index = startIndex
        while (index < possibleAccountList.size) {
            try {
                Log.d("Get account balance for index $index")
                val accountBalance =
                    proxyRepository.getAccountBalanceSuspended(possibleAccountList[index].accountAddress)
                Log.d("AccountBalance: $accountBalance")
                if (!accountBalance.accountExists()) {
                    Log.d("Unused account address")
                    return index
                }
                index++
            } catch (e: Exception) {
                val ex = BackendErrorHandler.getCoroutineBackendException(e)
                return if (ex != null && ex is BackendErrorException) {
                    Log.d("Backend error - unused account address", ex)
                    index
                } else {
                    // Other exceptions like connection problems should not let the account be created
                    Log.e("Unexpected exception when getting account balance", e)
                    null
                }
            }
        }
        return null
    }

    private fun submitCredential(credentialWrapper: CredentialWrapper) {
        _waitingLiveData.postValue(true)
        submitCredentialRequest?.dispose()
        submitCredentialRequest = proxyRepository.submitCredential(credentialWrapper,
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
        )
        saveNewAccount(newAccount)
    }

    private fun saveNewAccount(account: Account) = viewModelScope.launch(Dispatchers.IO) {
        val accountId = accountRepository.insert(account)
        account.id = accountId.toInt()
        // Also save a recipient representing this account
        recipientRepository.insert(Recipient(account))
        _gotoAccountCreatedLiveData.postValue(Event(account))
    }
}
