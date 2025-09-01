@file:Suppress("ConvertObjectToDataObject")

package com.concordium.wallet.ui.more.moreoverview

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

class MoreOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val walletSetupPreferences = App.appCore.session.walletStorage.setupPreferences
    private val accountRepository: AccountRepository by lazy {
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    }

    private val _seedRecoveryVisibilityLiveData = MutableLiveData<Boolean>()
    val seedRecoveryVisibilityLiveData: LiveData<Boolean> = _seedRecoveryVisibilityLiveData
    private val _fileImportExportVisibilityLiveData = MutableLiveData<Boolean>()
    val fileImportExportVisibilityLiveData: LiveData<Boolean> = _fileImportExportVisibilityLiveData
    private val _seedPhraseRevealVisibilityLiveData = MutableLiveData<Boolean>()
    val seedPhraseRevealVisibilityLiveData: LiveData<Boolean> = _seedPhraseRevealVisibilityLiveData
    private val _seedRevealVisibilityLiveData = MutableLiveData<Boolean>()
    val seedRevealVisibilityLiveData: LiveData<Boolean> = _seedRevealVisibilityLiveData
    private val _walletEraseVisibilityLiveData = MutableLiveData<Boolean>()
    private val _unshieldingVisibilityLiveData = MutableLiveData(false)
    val unshieldingVisibilityLiveData: LiveData<Boolean> = _unshieldingVisibilityLiveData
    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean> = _waitingLiveData
    private val _passwordAlterVisibilityLiveData = MutableLiveData<Boolean>()
    private val mutableEventsFlow =
        MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = mutableEventsFlow

    fun initialize() {
        updateOptionsVisibility()
    }

    fun updateOptionsVisibility() = viewModelScope.launch {
        // Seed recovery is visible when there is a seed.
        _seedRecoveryVisibilityLiveData.postValue(walletSetupPreferences.hasEncryptedSeed())
        // File import and export are visible if the backup is possible.
        _fileImportExportVisibilityLiveData.postValue(App.appCore.session.isAccountsBackupPossible())
        // Seed phrase reveal is visible when there is an encrypted saved version of it.
        _seedPhraseRevealVisibilityLiveData.postValue(walletSetupPreferences.hasEncryptedSeedPhrase())
        // Seed reveal is visible when there is an encrypted saved version of it,
        // while the phrase is not available.
        _seedRevealVisibilityLiveData.postValue(
            walletSetupPreferences.hasEncryptedSeed() && !walletSetupPreferences.hasEncryptedSeedPhrase()
        )
        // Wallet erase is visible if the initial setup is completed.
        _walletEraseVisibilityLiveData.postValue(App.appCore.setup.isInitialSetupCompleted)
        // Password alter is visible if it is set up.
        _passwordAlterVisibilityLiveData.postValue(App.appCore.setup.isAuthSetupCompleted)
        // Unshielding is visible if may be required.
        _unshieldingVisibilityLiveData.postValue(
            accountRepository.getAllDone().any(Account::mayNeedUnshielding)
        )
    }

    fun deleteWCDatabaseAndExit() {
        try {
            val path = getApplication<App>().dataDir.absolutePath
            File("$path/databases/WalletConnectAndroidCore.db").delete()
            File("$path/databases/WalletConnectV2.db").delete()
            Handler(Looper.getMainLooper()).post {
                exitProcess(0)
            }
        } catch (ex: Exception) {
            println(ex.stackTraceToString())
        }
    }

    fun onEraseContinueClicked() {
        mutableEventsFlow.tryEmit(Event.ShowAuthentication)
    }

    fun onAuthenticated() {
        eraseDataAndGoToWelcome()
    }

    private fun eraseDataAndGoToWelcome() {
        try {
            clearNotificationToken()
            with(getApplication<App>()) {
                dataDir.deleteRecursively()
                filesDir.deleteRecursively()
                noBackupFilesDir.deleteRecursively()
                cacheDir.deleteRecursively()
                File(filesDir.parentFile!!.absolutePath, "shared_prefs").deleteRecursively()
                initAppCore()
            }

            mutableEventsFlow.tryEmit(Event.ShowDataErasedMessage)
            mutableEventsFlow.tryEmit(Event.GoToWelcome)

            Handler(Looper.getMainLooper()).post {
                exitProcess(0)
            }
        } catch (ex: Exception) {
            println(ex.stackTraceToString())
        }
    }


    private fun clearNotificationToken() {
        try {
            FirebaseMessaging.getInstance().deleteToken()
        } catch (error: Exception) {
            Log.e("failed_deleting_notification_token", error)
        }
    }

    sealed interface Event {
        object ShowAuthentication : Event
        object ShowDataErasedMessage : Event
        object GoToWelcome : Event
    }
}
