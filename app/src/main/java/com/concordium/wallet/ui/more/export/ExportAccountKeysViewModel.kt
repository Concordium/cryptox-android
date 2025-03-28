package com.concordium.wallet.ui.more.export

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.data.cryptolib.StorageAccountData
import com.concordium.wallet.data.model.AccountData
import com.concordium.wallet.data.model.AccountDataKeys
import com.concordium.wallet.data.model.AccountKeys
import com.concordium.wallet.data.model.Credentials
import com.concordium.wallet.data.model.ExportAccountKeys
import com.concordium.wallet.data.model.Value
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.FileUtil
import com.concordium.wallet.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.launch

class ExportAccountKeysViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var account: Account
    lateinit var accountDataKeys: AccountDataKeys

    val textResourceInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val accountData: MutableLiveData<AccountData> by lazy { MutableLiveData<AccountData>() }

    fun saveFileToLocalFolder(destinationUri: Uri) = viewModelScope.launch {
        try {
            val accountKeys = AccountKeys(accountDataKeys, account.credential?.getThreshold() ?: 1)
            val credentials = Credentials(checkNotNull(account.credential?.getCredId()) {
                "The credential must have an ID"
            })
            val value = Value(accountKeys, credentials, account.address)
            val fileContent = Gson().toJson(
                ExportAccountKeys(
                    "concordium-browser-wallet-account",
                    account.credential?.v ?: 0,
                    BuildConfig.EXPORT_CHAIN,
                    value
                )
            )
            FileUtil.writeFile(destinationUri, "${account.address}.export", fileContent)
            textResourceInt.postValue(R.string.export_account_keys_file_exported)
        } catch (e: Exception) {
            Log.e("File export failed", e)

            textResourceInt.postValue(R.string.app_error_general)
        }
    }

    fun continueWithPassword(password: String) = viewModelScope.launch {
        decryptAndContinue(password)
    }

    private suspend fun decryptAndContinue(password: String) {
        val storageAccountDataEncrypted = account.encryptedAccountData
        if (storageAccountDataEncrypted == null) {
            textResourceInt.postValue(R.string.app_error_general)
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
            accountData.postValue(credentialsOutput.accountKeys)
        } else {
            textResourceInt.postValue(R.string.app_error_encryption)
        }
    }
}
