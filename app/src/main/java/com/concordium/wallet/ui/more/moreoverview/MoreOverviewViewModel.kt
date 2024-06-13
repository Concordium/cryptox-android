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
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.WalletDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

class MoreOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val authPreferences = AuthPreferences(application)
    private val accountRepository: AccountRepository by lazy {
        val accountDao = WalletDatabase.getDatabase(application).accountDao()
        AccountRepository(accountDao)
    }

    private val _seedRecoveryVisibilityLiveData = MutableLiveData<Boolean>()
    val seedRecoveryVisibilityLiveData: LiveData<Boolean> = _seedRecoveryVisibilityLiveData
    private val _fileImportExportVisibilityLiveData = MutableLiveData<Boolean>()
    val fileImportExportVisibilityLiveData: LiveData<Boolean> = _fileImportExportVisibilityLiveData
    private val _seedPhraseRevealVisibilityLiveData = MutableLiveData<Boolean>()
    val seedPhraseRevealVisibilityLiveData: LiveData<Boolean> = _seedPhraseRevealVisibilityLiveData
    private val _walletEraseVisibilityLiveData = MutableLiveData<Boolean>()
    val walletEraseVisibilityLiveData: LiveData<Boolean> = _walletEraseVisibilityLiveData
    private val _unshieldingVisibilityLiveData = MutableLiveData(false)
    val unshieldingVisibilityLiveData: LiveData<Boolean> = _unshieldingVisibilityLiveData
    private val _waitingLiveData = MutableLiveData<Boolean>()
    private val _passwordAlterVisibilityLiveData = MutableLiveData<Boolean>()
    val passwordAlterVisibilityLiveData: LiveData<Boolean> = _passwordAlterVisibilityLiveData
    val waitingLiveData: LiveData<Boolean> = _waitingLiveData
    private val mutableEventsFlow =
        MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = mutableEventsFlow

    fun initialize() {
        updateOptionsVisibility()
    }

    fun updateOptionsVisibility() = viewModelScope.launch {
        // Seed recovery is visible when there is a seed.
        _seedRecoveryVisibilityLiveData.postValue(authPreferences.hasEncryptedSeed())
        // File import and export are visible if the backup is possible.
        _fileImportExportVisibilityLiveData.postValue(App.appCore.session.isAccountsBackupPossible())
        // Seed phrase reveal is visible when there is an encrypted saved version of it.
        _seedPhraseRevealVisibilityLiveData.postValue(authPreferences.hasEncryptedSeedPhrase())
        // Wallet erase is visible if the initial setup is completed.
        _walletEraseVisibilityLiveData.postValue(App.appCore.session.hasCompletedInitialSetup)
        // Password alter is visible if it is set up.
        _passwordAlterVisibilityLiveData.postValue(authPreferences.getHasSetupUser())
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

    fun onAuthenticated(password: String) {
        eraseWalletAndGoToWelcome()
    }

    private fun eraseWalletAndGoToWelcome() {
        try {
            with(getApplication<App>()) {
                dataDir.deleteRecursively()
                filesDir.deleteRecursively()
                cacheDir.deleteRecursively()
                File(filesDir.parentFile!!.absolutePath, "shared_prefs").deleteRecursively()
                initAppCore()
            }

            mutableEventsFlow.tryEmit(Event.ShowWalletErasedMessage)
            mutableEventsFlow.tryEmit(Event.GoToWelcome)

            Handler(Looper.getMainLooper()).post {
                exitProcess(0)
            }
        } catch (ex: Exception) {
            println(ex.stackTraceToString())
        }
    }

    sealed interface Event {
        object ShowAuthentication : Event
        object ShowWalletErasedMessage : Event
        object GoToWelcome : Event
    }
}
