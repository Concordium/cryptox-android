package com.concordium.wallet.ui.more.import

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.Session
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.backend.BackendErrorException
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.core.multiwallet.SwitchActiveWalletTypeUseCase
import com.concordium.wallet.core.notifications.UpdateNotificationsSubscriptionUseCase
import com.concordium.wallet.core.security.EncryptionException
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.backend.repository.IdentityProviderRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.cryptolib.GenerateAccountsInput
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.export.AccountExport
import com.concordium.wallet.data.export.EncryptedExportData
import com.concordium.wallet.data.export.ExportData
import com.concordium.wallet.data.export.IdentityExport
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.model.GlobalParams
import com.concordium.wallet.data.model.IdentityAttribute
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.model.PossibleAccount
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.util.ExportEncryptionHelper
import com.concordium.wallet.ui.cis2.defaults.DefaultFungibleTokensManager
import com.concordium.wallet.ui.cis2.defaults.DefaultTokensManagerFactory
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.uicore.Formatter
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.RandomUtil
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.math.BigInteger

class ImportViewModel(application: Application) :
    AndroidViewModel(application) {

    private val session: Session = App.appCore.session
    private val identityProviderRepository = IdentityProviderRepository()
    private val proxyRepository = ProxyRepository()
    private val identityRepository: IdentityRepository =
        IdentityRepository(session.walletStorage.database.identityDao())
    private val accountRepository: AccountRepository =
        AccountRepository(session.walletStorage.database.accountDao())
    private val recipientRepository: RecipientRepository =
        RecipientRepository(session.walletStorage.database.recipientDao())
    private val defaultFungibleTokensManager: DefaultFungibleTokensManager

    private val gson = App.appCore.gson

    private var encryptedExportData: EncryptedExportData? = null
    private var importPassword: String? = null
    var importResult = ImportResult()
    private val nextIdentityIndicesByProviderId = mutableMapOf<Int, Int>()

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _errorAndFinishLiveData = MutableLiveData<Event<Int>>()
    val errorAndFinishLiveData: LiveData<Event<Int>>
        get() = _errorAndFinishLiveData

    private val _showImportPasswordLiveData = MutableLiveData<Event<Boolean>>()
    val showImportPasswordLiveData: LiveData<Event<Boolean>>
        get() = _showImportPasswordLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _showImportConfirmedLiveData = MutableLiveData<Event<Boolean>>()
    val showImportConfirmedLiveData: LiveData<Event<Boolean>>
        get() = _showImportConfirmedLiveData

    // True for successful finish, false otherwise.
    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData

    private val updateNotificationsSubscriptionUseCase by lazy(::UpdateNotificationsSubscriptionUseCase)

    init {
        val defaultTokensManagerFactory = DefaultTokensManagerFactory(
            contractTokensRepository = ContractTokensRepository(
                App.appCore.session.walletStorage.database.contractTokenDao()
            ),
        )
        defaultFungibleTokensManager = defaultTokensManagerFactory.getDefaultFungibleTokensManager()
    }

    fun initialize() {
    }

    fun checkPasswordRequirements(password: String): Boolean {
        return (password.length >= 6)
    }

    fun handleImportFile(importFile: ImportFile) = viewModelScope.launch {
        var fileContent: String
        try {
            fileContent = importFile.getContentAsString(getApplication())
        } catch (e: IOException) {
            Log.e("Could not read file")
            _errorAndFinishLiveData.postValue(Event(R.string.import_error_file))
            _waitingLiveData.postValue(false)
            return@launch
        }
        try {
            val encryptedExportData = gson.fromJson(fileContent, EncryptedExportData::class.java)
            if (encryptedExportData.hasRequiredData()) {
                this@ImportViewModel.encryptedExportData = encryptedExportData
                _showImportPasswordLiveData.postValue(Event(true))
            } else {
                _errorAndFinishLiveData.postValue(Event(R.string.app_error_json))
            }
        } catch (e: Exception) {
            _errorAndFinishLiveData.postValue(Event(R.string.app_error_json))
        }
        _waitingLiveData.postValue(false)
    }

    fun startImport(password: String) {
        importPassword = password
        _showAuthenticationLiveData.postValue(Event(true))
    }

    fun checkLogin(password: String) = viewModelScope.launch {
        _waitingLiveData.postValue(true)
        handleAuthPassword(password)
    }

    private suspend fun handleAuthPassword(password: String) {
        // Decrypt the master key
        val decryptedMasterKey = runCatching {
            App.appCore.auth.getMasterKey(password)
        }.getOrNull()
        if (decryptedMasterKey == null) {
            _errorLiveData.postValue(Event(R.string.app_error_encryption))
            _waitingLiveData.postValue(false)
            return
        }
        performImport(encryptedExportData, importPassword, decryptedMasterKey)
    }

    private suspend fun performImport(
        encryptedExportData: EncryptedExportData?,
        importPassword: String?,
        masterKey: ByteArray,
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (encryptedExportData == null || importPassword == null) {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            return@launch
        }
        var exportData: ExportData?
        try {
            val decryptedImportData =
                ExportEncryptionHelper.decryptExportData(importPassword, encryptedExportData)
            exportData = gson.fromJson(decryptedImportData, ExportData::class.java)
        } catch (e: IllegalArgumentException) {
            _waitingLiveData.postValue(false)
            _errorAndFinishLiveData.postValue(Event(R.string.import_error_password_or_file_content))
            return@launch
        } catch (e: Exception) {
            _waitingLiveData.postValue(false)
            when (e) {
                is JsonIOException,
                is JsonSyntaxException,
                -> {
                    Log.e("Unexpected json format")
                    _errorAndFinishLiveData.postValue(Event(R.string.import_error_password_or_file_content))
                }

                is EncryptionException -> {
                    Log.e("Unexpected encryption/decryption error")
                    _errorAndFinishLiveData.postValue(Event(R.string.import_error_password_or_file_content))
                }

                else -> throw e
            }
            return@launch
        }
        if (exportData == null || !exportData.hasRequiredData()) {
            _waitingLiveData.postValue(false)
            _errorAndFinishLiveData.postValue(Event(R.string.app_error_json))
            return@launch
        }

        if (!exportData.hasRequiredIdentities()) {
            _waitingLiveData.postValue(false)
            _errorAndFinishLiveData.postValue(Event(R.string.app_import_missing_identities))
            return@launch
        }

        val allowedEnvironments =
        // To eliminate confusion of "CryptoX Stage" working on testnet chain,
            // allow both testnet and stage exports to be used when using testnet or stage.
            if (BuildConfig.ENV_NAME == "staging" || BuildConfig.ENV_NAME == "testnet")
                setOf("staging", "testnet")
            else
                setOf(BuildConfig.EXPORT_CHAIN)

        if (exportData.environment !in allowedEnvironments) {
            _waitingLiveData.postValue(false)
            _errorAndFinishLiveData.postValue(Event(R.string.app_error_wrong_environment))
            return@launch
        }

        SwitchActiveWalletTypeUseCase().invoke(
            newWalletType = AppWallet.Type.FILE,
        )
        App.appCore.setup.finishInitialSetup()
        App.appCore.session.walletStorage.setupPreferences.setHasCompletedOnboarding(true)

        val exportValue = exportData.value
        importResult = ImportResult()
        // Recipients
        val existingRecipientList = recipientRepository.getAll()
        val recipientList = mutableListOf<Recipient>()
        // There may be duplicates caused by old bugs that should be filtered out.
        // As long as RecipientExport is a data class, distinct does the thing,
        // also preserving the original order.
        for (recipientExport in exportValue.recipients.distinct()) {
            try {
                val recipient = Recipient(0, recipientExport.name, recipientExport.address)
                val isDuplicate = existingRecipientList.any { existingRecipient ->
                    existingRecipient.name == recipient.name && existingRecipient.address == recipient.address
                }
                if (!isDuplicate) {
                    recipientList.add(recipient)
                }
                importResult.addRecipientResult(
                    ImportResult.RecipientImportResult(
                        recipient.name,
                        if (isDuplicate) ImportResult.Status.Duplicate else ImportResult.Status.Ok
                    )
                )
            } catch (e: Exception) {
                // In case the mandatory fields are not present
                importResult.addRecipientResult(
                    ImportResult.RecipientImportResult(
                        "",
                        ImportResult.Status.Failed
                    )
                )
            }
        }
        recipientRepository.insertAll(recipientList)
        // Identities and accounts
        val existingIdentityList = identityRepository.getAll()
        val existingAccountList = accountRepository.getAll()
        nextIdentityIndicesByProviderId.clear()
        exportValue.identities.forEach { identityExport ->
            var identity: Identity? = null
            var identityId: Long? = null
            var identityImportResult =
                ImportResult.IdentityImportResult("", ImportResult.Status.Failed)
            try {
                identity = encryptAndMapIdentity(identityExport, masterKey)
            } catch (e: Exception) {
                // In case the mandatory fields are not present
                // Setting state is handled below
            }
            if (identity == null) {
                importResult.addIdentityResult(identityImportResult)
            } else {
                // Find existing/duplicate Identity (based on idCredPub)
                val oldIdentity = existingIdentityList.firstOrNull { existingIdentity ->
                    // Avoid that two identities without an IdentityObject gets matched
                    !TextUtils.isEmpty(existingIdentity.identityObject?.preIdentityObject?.pubInfoForIp?.idCredPub) &&
                            existingIdentity.identityObject?.preIdentityObject?.pubInfoForIp?.idCredPub == identity.identityObject?.preIdentityObject?.pubInfoForIp?.idCredPub
                }
                // Only save the imported identity, if it is not a duplicate
                identityId = oldIdentity?.id?.toLong() ?: identityRepository.insert(identity)
                val status =
                    if (oldIdentity == null) ImportResult.Status.Ok else ImportResult.Status.Duplicate
                identityImportResult = ImportResult.IdentityImportResult(identity.name, status)
                importResult.addIdentityResult(identityImportResult)
            }
            // Accounts
            val accountList = mutableListOf<Account>()
            if (hasAccountList(identityExport)) {
                identityExport.accounts.forEachIndexed { credNumber, accountExport ->
                    var account: Account? = null
                    if (identityId != null) {
                        try {
                            // Credential number for an account can be set to the account index,
                            // it doesn't matter while using legacy credentials creation.
                            //
                            // It may, however, break the derivation once the seed phrase feature
                            // is added. In this case, there should be no merging import.
                            account = encryptAndMapAccount(
                                accountExport,
                                identityId,
                                credNumber,
                                masterKey
                            )
                        } catch (e: Exception) {
                            // In case the mandatory fields are not present
                            // Setting state is handled below
                        }
                    }
                    if (account == null) {
                        val accountImportResult =
                            ImportResult.AccountImportResult("", ImportResult.Status.Failed)
                        identityImportResult.addAccountResult(accountImportResult)
                    } else {
                        var status = ImportResult.Status.Ok
                        val existingDuplicate =
                            existingAccountList.firstOrNull() { existingAccount -> existingAccount.address == account.address }
                        if (existingDuplicate == null) {
                            accountList.add(account)
                        } else {
                            // Replace with imported account if the existing is readonly - update the fields that are part of the AccountExport
                            if (existingDuplicate.readOnly) {
                                existingDuplicate.readOnly = false
                                existingDuplicate.name = account.name
                                existingDuplicate.submissionId = account.submissionId
                                existingDuplicate.encryptedAccountData =
                                    account.encryptedAccountData
                                existingDuplicate.revealedAttributes =
                                    account.revealedAttributes
                                existingDuplicate.credential = account.credential
                                accountRepository.update(existingDuplicate)
                            } else {
                                status = ImportResult.Status.Duplicate
                            }
                        }
                        val accountImportResult =
                            ImportResult.AccountImportResult(account.name, status)
                        identityImportResult.addAccountResult(accountImportResult)
                    }
                }
                accountRepository.insertAll(accountList)

                // Add default fungible tokens for the new accounts.
                // Updated read-only accounts are not affected,
                // as their tokens may be already managed by the user.
                accountList.forEach { account ->
                    defaultFungibleTokensManager.addForAccount(account.address)
                }
                if (accountList.isNotEmpty()) {
                    updateNotificationsSubscriptionUseCase()
                }
            }
            // Read-only accounts - even though there are no accounts in the import file, there can be accounts from other devices
            // The account list used to check for existing account must include the ones that was just added for this identity
            if (identityId != null) {
                accountList.addAll(existingAccountList)
                handleReadOnlyAccounts(
                    accountList,
                    identityExport,
                    identityId,
                    identityImportResult
                )
            }
        } // End of identity
        activateFirstAccountIfNoneActive()
        confirmImport()
        _waitingLiveData.postValue(false)
    }

    private suspend fun handleReadOnlyAccounts(
        existingAccountList: List<Account>,
        identityExport: IdentityExport,
        identityId: Long,
        identityImportResult: ImportResult.IdentityImportResult,
    ) {
        var globalParams: GlobalParams?
        try {
            val globalParamsWrapper = identityProviderRepository.getGlobalInfoSuspended()
            globalParams = globalParamsWrapper.value
        } catch (e: Exception) {
            return
        }
        val generateAccountsInput = GenerateAccountsInput(
            globalParams,
            identityExport.identityObject,
            identityExport.privateIdObjectData
        )
        val possibleAccountList =
            App.appCore.cryptoLibrary.generateAccounts(generateAccountsInput) ?: return
        Log.d("Generated account info for ${possibleAccountList.size} accounts")
        checkExistingAccountsForReadOnly(
            possibleAccountList,
            0,
            existingAccountList,
            identityId,
            identityImportResult
        )
    }

    private suspend fun checkExistingAccountsForReadOnly(
        possibleAccountList: List<PossibleAccount>,
        startIndex: Int,
        existingAccountList: List<Account>,
        identityId: Long,
        identityImportResult: ImportResult.IdentityImportResult,
    ) {
        val readOnlyAccountList = mutableListOf<Account>()
        var index = startIndex
        while (index < possibleAccountList.size) {
            try {
                val possibleAccount = possibleAccountList[index]
                val accountBalance =
                    proxyRepository.getAccountBalanceSuspended(possibleAccount.accountAddress)
                Log.d("Got account balance for index $index and accountAddress ${possibleAccount.accountAddress} accountBalance $accountBalance")
                if (accountBalance.accountExists()) {
                    // Check if the account exists
                    val isAccountDuplicate =
                        existingAccountList.any { existingAccount -> existingAccount.address == possibleAccount.accountAddress }
                    if (!isAccountDuplicate) {
                        readOnlyAccountList.add(createReadOnlyAccount(possibleAccount, identityId))
                        val name = Formatter.formatAsFirstEight(possibleAccount.accountAddress)
                        identityImportResult.addReadOnlyAccountResult(
                            ImportResult.AccountImportResult(
                                name,
                                ImportResult.Status.Ok
                            )
                        )
                    }
                } else {
                    Log.d("Unused account address - no need to continue")
                }
                index++
            } catch (e: Exception) {
                val ex = BackendErrorHandler.getCoroutineBackendException(e)
                if (ex != null && ex is BackendErrorException) {
                    Log.d("Backend error - unused account address", ex)
                } else {
                    // Other exceptions like connection problems - just continue
                    Log.e("Unexpected exception when getting account balance", e)
                    index++
                }
            }
        }
        accountRepository.insertAll(readOnlyAccountList)

        val existingRecipientList = recipientRepository.getAll()
        val readOnlyRecipients = readOnlyAccountList
            .filterNot {
                // Avoid making duplicates for read-only accounts,
                // but check only by the address as the name is auto-generated.
                existingRecipientList.any { existingRecipient ->
                    existingRecipient.address == it.address
                }
            }
            .map(::Recipient)
        recipientRepository.insertAll(readOnlyRecipients)

        // Add default fungible tokens for the accounts.
        readOnlyAccountList.forEach { account ->
            defaultFungibleTokensManager.addForAccount(account.address)
        }
        if (readOnlyAccountList.isNotEmpty()) {
            updateNotificationsSubscriptionUseCase()
        }
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun hasAccountList(identityExport: IdentityExport): Boolean {
        // Because the data is parsed by Gson, the non-nullable members can actually be null
        return identityExport.accounts != null
    }

    private suspend fun encryptAndMapIdentity(
        identityExport: IdentityExport,
        masterKey: ByteArray,
    ): Identity? {
        val privateIdObjectDataJson = gson.toJson(identityExport.privateIdObjectData)
        val privateIdObjectDataEncrypted = App.appCore.auth
            .encrypt(
                masterKey = masterKey,
                data = privateIdObjectDataJson.toByteArray(),
            )
        if (privateIdObjectDataEncrypted == null) {
            Log.e("Could not encrypt privateIdObjectData for identity: ${identityExport.name}")
            return null
        } else {
            return mapIdentityFromExport(
                identityExport,
                privateIdObjectDataEncrypted
            )
        }
    }

    private suspend fun encryptAndMapAccount(
        accountExport: AccountExport,
        identityId: Long,
        credNumber: Int,
        masterKey: ByteArray,
    ): Account? {

        val accountDataJson = gson.toJson(
            StorageAccountData(
                accountExport.address,
                accountExport.accountKeys,
                accountExport.encryptionSecretKey
            )
        )
        val accountDataEncrypted = App.appCore.auth
            .encrypt(
                masterKey = masterKey,
                data = accountDataJson.toByteArray(),
            )
        if (accountDataEncrypted == null) {
            Log.e("Could not encrypt accountData for account: ${accountExport.name}")
            return null
        } else {
            return mapAccountFromExport(accountExport, identityId, credNumber, accountDataEncrypted)
        }
    }

    private suspend fun mapIdentityFromExport(
        identityExport: IdentityExport,
        privateIdObjectDataEncrypted: EncryptedData,
    ): Identity {
        // Correct indices for identities are important
        // when importing on top of the existing identities.
        val identityProviderId = identityExport.identityProvider.ipInfo.ipIdentity
        val identityIndex = nextIdentityIndicesByProviderId.getOrPut(identityProviderId) {
            identityRepository.nextIdentityIndex(identityProviderId)
        }
        nextIdentityIndicesByProviderId[identityProviderId] = identityIndex + 1

        return Identity(
            id = 0,
            name = identityExport.name,
            status = IdentityStatus.DONE,
            detail = "",
            codeUri = "",
            nextAccountNumber = identityExport.nextAccountNumber,
            identityProvider = identityExport.identityProvider,
            identityObject = identityExport.identityObject,
            privateIdObjectDataEncrypted = privateIdObjectDataEncrypted,
            identityProviderId = identityProviderId,
            identityIndex = identityIndex,
        )
    }

    private fun mapAccountFromExport(
        accountExport: AccountExport,
        identityId: Long,
        credNumber: Int,
        accountDataEncrypted: EncryptedData,
    ): Account {
        return Account(
            identityId = identityId.toInt(),
            name = accountExport.name,
            address = accountExport.address,
            submissionId = accountExport.submissionId,
            transactionStatus = TransactionStatus.FINALIZED,
            encryptedAccountData = accountDataEncrypted,
            revealedAttributes = mapRevealedAttributes(accountExport.revealedAttributes),
            credential = accountExport.credential,
            credNumber = credNumber,
            iconId = RandomUtil.getRandomInt()
        )
    }

    private fun createReadOnlyAccount(
        possibleAccount: PossibleAccount,
        identityId: Long,
    ): Account {
        return Account(
            identityId = identityId.toInt(),
            name = Account.getDefaultName(possibleAccount.accountAddress),
            address = possibleAccount.accountAddress,
            submissionId = "",
            transactionStatus = TransactionStatus.FINALIZED,
            encryptedAccountData = null,
            credential = null,
            readOnly = true,
            credNumber = 0,
            iconId = RandomUtil.getRandomInt()
        )
    }

    private fun mapRevealedAttributes(map: HashMap<String, String>): List<IdentityAttribute> {
        val list = mutableListOf<IdentityAttribute>()
        for ((k, v) in map) {
            list.add(IdentityAttribute(k, v))
        }
        return list
    }

    private fun confirmImport() {
        _showImportConfirmedLiveData.postValue(Event(true))
    }

    fun finishImport(isSuccessful: Boolean) {
        _finishScreenLiveData.postValue(Event(isSuccessful))
    }

    private suspend fun activateFirstAccountIfNoneActive() {
        val allAccounts = accountRepository.getAll()
        val activeAccount = accountRepository.getActive()

        if (allAccounts.isNotEmpty() && activeAccount == null) {
            accountRepository.activate(
                allAccounts
                    .sortedBy { it.id }
                    .first().address
            )

            allAccounts
                .find { it.balance > BigInteger.ZERO }
                ?.let {
                    App.appCore.setup.setHasShowReviewDialogAfterReceiveFunds(true)
                }
            App.appCore.session.walletStorage.setupPreferences.setShowReviewDialogSnapshotTime()
        }
    }
}
