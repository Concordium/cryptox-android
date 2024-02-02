package com.concordium.wallet.ui.more.alterpassword

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import cash.z.ecc.android.bip39.Mnemonics
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.util.HexUtil.toHex
import com.concordium.wallet.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.crypto.SecretKey

class AlterPasswordViewModel(application: Application) :
    AndroidViewModel(application) {

    private var inititalDecryptedAccountsList: ArrayList<Account> = ArrayList()
    private var inititalDecryptedIdentityList: List<Identity> = emptyList()

    private var decryptedSeedHex: String? = null
    private var decryptedSeedPhrase: String? = null

    private var database: WalletDatabase
    private val authPreferences: AuthPreferences

    private val identityRepository: IdentityRepository
    private val accountRepository: AccountRepository

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _checkAccountsIdentitiesDoneLiveData = MutableLiveData<Boolean>()
    val checkAccountsIdentitiesDoneLiveData: LiveData<Boolean>
        get() = _checkAccountsIdentitiesDoneLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _showAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val showAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _showAuthenticationLiveData

    // Final encryption flow
    private val _doneFinalChangePasswordLiveData = MutableLiveData<Event<Boolean>>()
    val doneFinalChangePasswordLiveData: LiveData<Event<Boolean>>
        get() = _doneFinalChangePasswordLiveData

    private val _errorFinalChangePasswordLiveData = MutableLiveData<Event<Boolean>>()
    val errorFinalChangePasswordLiveData: LiveData<Event<Boolean>>
        get() = _errorFinalChangePasswordLiveData

    // Initial decryption flow
    private val _doneInitialAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val doneInitialAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _doneInitialAuthenticationLiveData

    private val _errorInitialAuthenticationLiveData = MutableLiveData<Event<Boolean>>()
    val errorInitialAuthenticationLiveData: LiveData<Event<Boolean>>
        get() = _errorInitialAuthenticationLiveData

    init {
        database = WalletDatabase.getDatabase(application)
        authPreferences = AuthPreferences(application)
        val identityDao = database.identityDao()
        identityRepository = IdentityRepository(identityDao)
        val accountDao = database.accountDao()
        accountRepository = AccountRepository(accountDao)
    }

    fun initialize() {
    }

    fun checkLogin(password: String) = viewModelScope.launch(Dispatchers.IO) {
        _waitingLiveData.postValue(true)
        handleAuthPassword(password)
    }

    private suspend fun handleAuthPassword(password: String) {
        // Decrypt the private data
        val key =
            App.appCore.getCurrentAuthenticationManager().derivePasswordKeyInBackground(password)
        if (key == null) {
            _errorLiveData.postValue(Event(R.string.app_error_encryption))
            _waitingLiveData.postValue(false)
            return
        }
        decryptOriginalAndContinue(key)
    }

    fun finishPasswordChange(newPassword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDecryptKey = App.appCore.getCurrentAuthenticationManager()
                .derivePasswordKeyInBackground(newPassword)
            if (newDecryptKey == null) {
                _errorLiveData.postValue(Event(R.string.app_error_encryption))
                _waitingLiveData.postValue(false)
            } else {
                encryptAndFinalize(newDecryptKey)
            }
        }
    }

    private fun decryptOriginalAndContinue(decryptKey: SecretKey) =
        viewModelScope.launch(Dispatchers.IO) {
            _waitingLiveData.postValue(true)
            var allSuccess = true
            try {
                inititalDecryptedIdentityList = identityRepository.getAllDone()
                for (identity in inititalDecryptedIdentityList) {
                    val tmpInititalDecryptedAccountsList =
                        accountRepository.getAllByIdentityId(identity.id)
                    for (account in tmpInititalDecryptedAccountsList) {

                        if (!account.encryptedAccountData.isEmpty()) {
                            val accountDataDecryped = App.appCore.getOriginalAuthenticationManager()
                                .decryptInBackground(decryptKey, account.encryptedAccountData)
                            if (accountDataDecryped != null && isJson(accountDataDecryped)) {
                                account.encryptedAccountData = accountDataDecryped
                            } else {
                                allSuccess = false
                            }
                        }
                        inititalDecryptedAccountsList.add(account)
                    }
                    // Identities have private ID object only when V0 key creation is used.
                    if (identity.privateIdObjectDataEncrypted.isNotEmpty()) {
                        val privateIdObjectDataDecryped =
                            App.appCore.getOriginalAuthenticationManager().decryptInBackground(
                                decryptKey,
                                identity.privateIdObjectDataEncrypted
                            )
                        if (privateIdObjectDataDecryped != null && isJson(
                                privateIdObjectDataDecryped
                            )
                        ) {
                            identity.privateIdObjectDataEncrypted = privateIdObjectDataDecryped
                        } else {
                            allSuccess = false
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                allSuccess = false
            }

            // Seed is present only when V1 key creation is used.
            if (authPreferences.hasEncryptedSeed()) {
                try {
                    decryptedSeedHex = authPreferences.getSeedHex(decryptKey)
                    decryptedSeedPhrase = authPreferences.getSeedPhrase(decryptKey)

                    // The phrase may not be there, but the seed is a must.
                    if (decryptedSeedHex == null) {
                        allSuccess = false
                    }

                    if (BuildConfig.DEBUG) {
                        Log.d(
                            "decrypted_seed:" +
                                    "\nhex=$decryptedSeedHex," +
                                    "\nphrase=$decryptedSeedPhrase"
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    allSuccess = false
                }
            }

            if (allSuccess) {
                App.appCore.startResetAuthFlow()
                _doneInitialAuthenticationLiveData.postValue(Event(true))
                _waitingLiveData.postValue(false)
            } else {
                _errorInitialAuthenticationLiveData.postValue(Event(true))
                _waitingLiveData.postValue(false)
            }
        }

    private fun encryptAndFinalize(encryptKey: SecretKey) =
        viewModelScope.launch(Dispatchers.IO) {
            _waitingLiveData.postValue(true)

            var allSuccess = true

            // Seed phrase is present only when V1 key creation is used
            // and it has been saved as an entropy.
            if (decryptedSeedPhrase != null) {
                try {
                    val entropyHex =
                        Mnemonics.MnemonicCode(decryptedSeedPhrase!!).toEntropy().toHex()
                    val entropyHexEncrypted = App.appCore.getOriginalAuthenticationManager()
                        .encryptInBackground(encryptKey, entropyHex)
                    if (entropyHexEncrypted == null
                        || !authPreferences.updateEncryptedSeedEntropyHex(entropyHexEncrypted)
                    ) {
                        allSuccess = false
                    }
                    decryptedSeedPhrase = null
                } catch (e: Exception) {
                    allSuccess = false
                }
            }

            // Seed is present only when V1 key creation is used.
            if (decryptedSeedHex != null) {
                try {
                    val seedHexEncrypted = App.appCore.getOriginalAuthenticationManager()
                        .encryptInBackground(encryptKey, decryptedSeedHex!!)
                    if (seedHexEncrypted == null
                        || !authPreferences.updateEncryptedSeedHex(seedHexEncrypted)
                    ) {
                        allSuccess = false
                    }
                    decryptedSeedHex = null
                } catch (e: Exception) {
                    allSuccess = false
                }
            }

            database.withTransaction {
                try {
                    for (account in inititalDecryptedAccountsList) {
                        if (!account.encryptedAccountData.isEmpty()) {
                            val accountDataEncryped = App.appCore.getOriginalAuthenticationManager()
                                .encryptInBackground(
                                    encryptKey,
                                    account.encryptedAccountData
                                ) // Which is decrypted by now!
                            if (accountDataEncryped != null) {
                                account.encryptedAccountData = accountDataEncryped
                                accountRepository.update(account)
                            } else {
                                allSuccess = false
                            }
                        }
                    }
                    for (identity in inititalDecryptedIdentityList) {
                        // Identities have private ID object only when V0 key creation is used.
                        if (identity.privateIdObjectDataEncrypted.isNotEmpty()) {
                            val privateIdObjectDataEncryped =
                                App.appCore.getOriginalAuthenticationManager().encryptInBackground(
                                    encryptKey,
                                    identity.privateIdObjectDataEncrypted
                                ) // Which is decrypted by now!
                            if (privateIdObjectDataEncryped != null) {
                                identity.privateIdObjectDataEncrypted = privateIdObjectDataEncryped
                                identityRepository.update(identity)
                            } else {
                                allSuccess = false
                            }
                        }
                    }

                    inititalDecryptedAccountsList = ArrayList()
                    inititalDecryptedIdentityList = emptyList()
                } catch (e: Exception) {
                    allSuccess = false
                }

                if (allSuccess) {
                    App.appCore.finalizeResetAuthFlow()
                    _doneFinalChangePasswordLiveData.postValue(Event(true))
                } else {
                    App.appCore.cancelResetAuthFlow()
                    _errorFinalChangePasswordLiveData.postValue(Event(true))
                    throw Exception()
                }
            }
        }

    private fun isJson(Json: String?): Boolean {
        val gson = Gson()
        return try {
            gson.fromJson(Json, Any::class.java)
            val jsonObjType: Any = gson.fromJson(Json, Any::class.java).javaClass
            jsonObjType != String::class.java
        } catch (ex: JsonSyntaxException) {
            false
        }
    }

    fun checkAndStartPasscodeChange() = viewModelScope.launch(Dispatchers.IO) {
        _checkAccountsIdentitiesDoneLiveData.postValue((identityRepository.getNonDoneCount() + accountRepository.getNonDoneCount()) == 0)
    }
}
