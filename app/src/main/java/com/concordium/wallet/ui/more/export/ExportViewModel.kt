package com.concordium.wallet.ui.more.export

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.export.AccountExport
import com.concordium.wallet.data.export.ExportData
import com.concordium.wallet.data.export.ExportValue
import com.concordium.wallet.data.export.IdentityExport
import com.concordium.wallet.data.export.RecipientExport
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.IdentityAttribute
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.util.ExportEncryptionHelper
import com.concordium.wallet.data.util.FileUtil
import com.concordium.wallet.util.Log
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

class ExportViewModel(application: Application) :
    AndroidViewModel(application) {

    companion object {
        const val FILE_NAME = "export.concordiumwallet"
    }

    private val identityRepository =
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    private val accountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    private val recipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())

    private val gson = App.appCore.gson
    private var _exportPassword: String = ""

    private var decryptedMasterKey: ByteArray? = null

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _errorPasswordLiveData = MutableLiveData<Event<Boolean>>()
    val errorPasswordLiveData: LiveData<Event<Boolean>>
        get() = _errorPasswordLiveData

    private val _errorExportLiveData = MutableLiveData<Event<List<String>>>()
    val errorExportLiveData: LiveData<Event<List<String>>>
        get() = _errorExportLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    private val _showRequestPasswordLiveData = MutableLiveData<Event<Boolean>>()
    val showRequestPasswordLiveData: LiveData<Event<Boolean>>
        get() = _showRequestPasswordLiveData

    private val _shareExportFileLiveData = MutableLiveData<Event<Boolean>>()
    val shareExportFileLiveData: LiveData<Event<Boolean>>
        get() = _shareExportFileLiveData

    private val _showRepeatPasswordScreenLiveData = MutableLiveData<Event<Boolean>>()
    val showRepeatPasswordScreenLiveData: LiveData<Event<Boolean>>
        get() = _showRepeatPasswordScreenLiveData

    private val _finishPasswordScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishPasswordScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishPasswordScreenLiveData

    private val _finishRepeatPasswordScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishRepeatPasswordScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishRepeatPasswordScreenLiveData

    private val _errorNonIdenticalRepeatPasswordLiveData = MutableLiveData<Event<Boolean>>()
    val errorNonIdenticalRepeatPasswordLiveData: LiveData<Event<Boolean>>
        get() = _errorNonIdenticalRepeatPasswordLiveData

    fun initialize() {
    }

    fun export(force: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val accountNamesFailed = mutableListOf<String>()
        val identityList = identityRepository.getAllDone()
        for (identity in identityList) {
            val accountList = accountRepository.getAllByIdentityId(identity.id)
            for (account in accountList) {
                if (account.transactionStatus != TransactionStatus.FINALIZED) {
                    accountNamesFailed.add(account.name)
                }
            }
        }

        if (!force && accountNamesFailed.size > 0) {
            _errorExportLiveData.postValue(Event(accountNamesFailed))
        } else {
            _showAuthenticationLiveData.postValue(Event(true))
        }
    }

    fun checkLogin(password: String) = viewModelScope.launch(Dispatchers.IO) {
        _waitingLiveData.postValue(true)
        handleAuthPassword(password)
    }

    private suspend fun handleAuthPassword(password: String) {
        // Decrypt the master key
        this.decryptedMasterKey = runCatching {
            App.appCore.auth.getMasterKey(password)
        }.getOrNull()
        if (decryptedMasterKey == null) {
            _errorLiveData.postValue(Event(R.string.app_error_encryption))
            _waitingLiveData.postValue(false)
            return
        }

        _showRequestPasswordLiveData.postValue(Event(true))
        _waitingLiveData.postValue(false)
    }

    fun finalizeEncryptionOfFile() {
        val decryptedMasterKey = decryptedMasterKey

        if (decryptedMasterKey != null) {
            decryptAndContinue(decryptedMasterKey, _exportPassword)
        } else {
            _errorLiveData.postValue(Event(R.string.app_error_general))
            return
        }
    }

    private fun decryptAndContinue(
        masterKey: ByteArray, exportPassword: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        _waitingLiveData.postValue(true)
        try {
            // Get all data, parse, decrypt and generate file content
            val identityExportList = mutableListOf<IdentityExport>()
            val failedAccountAddressExportList = mutableListOf<String>()
            val identityList = identityRepository.getAllDone()
            for (identity in identityList) {
                val accountExportList = mutableListOf<AccountExport>()
                val accountList = accountRepository.getAllByIdentityId(identity.id)
                for (account in accountList) {
                    val storageAccountDataEncrypted = account.encryptedAccountData
                    if (account.readOnly || storageAccountDataEncrypted == null) {
                        // Skip read only accounts (they will be found when importing)
                        continue
                    }
                    if (account.transactionStatus == TransactionStatus.FINALIZED) {
                        val accountDataJsonDecrypted =
                            App.appCore.auth
                                .decrypt(
                                    masterKey = masterKey,
                                    encryptedData = storageAccountDataEncrypted,
                                )
                                ?.let(::String)
                                ?: error("Failed decrypting account data: ${account.address}")
                        val accountData =
                            gson.fromJson(
                                accountDataJsonDecrypted,
                                StorageAccountData::class.java
                            )
                        val accountExport = mapAccountToExport(account, accountData)
                        accountExportList.add(accountExport)
                    } else {
                        failedAccountAddressExportList.add(account.address)
                    }
                }

                val privateIdObjectDataDecrypted =
                    App.appCore.auth
                        .decrypt(
                            masterKey = masterKey,
                            encryptedData = identity.privateIdObjectDataEncrypted
                                ?: error("V0 identity must have encrypted private ID object data")
                        )
                        ?.let(::String)
                        ?: error("Failed decrypting ID private object data: ${identity.name}")
                val privateIdObjectData =
                    gson.fromJson(privateIdObjectDataDecrypted, RawJson::class.java)
                val identityExport =
                    mapIdentityToExport(identity, privateIdObjectData, accountExportList)
                identityExportList.add(identityExport)
            }
            // There may be duplicates caused by old bugs that should be filtered out.
            // As long as RecipientExport is a data class, LinkedHashSet does the thing,
            // also preserving the original order.
            val recipientExportSet = linkedSetOf<RecipientExport>()
            val recipientList = recipientRepository.getAll()
            for (recipient in recipientList) {
                if (!failedAccountAddressExportList.contains(recipient.address)) {
                    recipientExportSet.add(RecipientExport(recipient.name, recipient.address))
                }
            }
            val exportValue = ExportValue(identityExportList, recipientExportSet.toList())
            val exportData = ExportData(
                "concordium-mobile-wallet-data",
                1,
                exportValue,
                BuildConfig.EXPORT_CHAIN
            )
            val jsonOutput = gson.toJson(exportData)
            Log.d("ExportData: $jsonOutput")

            // Encrypt
            val encryptedExportData =
                ExportEncryptionHelper.encryptExportData(exportPassword, jsonOutput)
            val fileContent = gson.toJson(encryptedExportData)

            // Save and share file
            FileUtil.saveFile(App.appContext, FILE_NAME, fileContent)
            _shareExportFileLiveData.postValue(Event(true))
            _waitingLiveData.postValue(false)
        } catch (e: Exception) {
            _waitingLiveData.postValue(false)
            when (e) {
                is JsonIOException,
                is JsonSyntaxException -> {
                    _errorLiveData.postValue(Event(R.string.app_error_json))
                }

                is FileNotFoundException -> {
                    _errorLiveData.postValue(Event(R.string.export_error_file))
                }

                else -> throw e
            }
        }
    }

    private fun mapIdentityToExport(
        identity: Identity,
        privateIdObjectData: RawJson,
        accountList: List<AccountExport>
    ): IdentityExport {
        return IdentityExport(
            identity.name,
            identity.nextAccountNumber,
            identity.identityProvider,
            identity.identityObject!!,
            privateIdObjectData,
            accountList
        )
    }

    private fun mapAccountToExport(
        account: Account,
        accountData: StorageAccountData
    ): AccountExport {
        return AccountExport(
            account.name,
            account.address,
            account.submissionId,
            accountData.accountKeys,
            mapRevealedAttributes(account.revealedAttributes),
            account.credential ?: CredentialWrapper(RawJson("{}"), 0),
            accountData.encryptionSecretKey
        )
    }

    private fun mapRevealedAttributes(list: List<IdentityAttribute>): HashMap<String, String> {
        val map = HashMap<String, String>()
        for (item in list) {
            map[item.name] = item.value
        }
        return map
    }

    fun checkPasswordRequirements(password: String): Boolean {
        return (password.length >= 6)
    }

    fun setStartExportPassword(password: String) {
        _exportPassword = password
        _showRepeatPasswordScreenLiveData.postValue(Event(true))
    }

    fun checkExportPassword(password: String) {
        val isEqual = _exportPassword.equals(password)
        if (!isEqual) {
            _errorNonIdenticalRepeatPasswordLiveData.postValue(Event(false))
        }
        _finishRepeatPasswordScreenLiveData.postValue(Event(isEqual))
    }
}
