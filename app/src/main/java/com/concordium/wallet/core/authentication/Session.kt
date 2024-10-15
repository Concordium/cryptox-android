package com.concordium.wallet.core.authentication

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.data.WalletStorage
import com.concordium.wallet.data.preferences.AppAuthPreferences
import com.concordium.wallet.data.preferences.Preferences

class Session(context: Context) {

    private val appAuthPreferences = AppAuthPreferences(context)
    val walletStorage = WalletStorage(context)

    var hasSetupPassword = walletStorage.setupPreferences.getHasSetupUser()
        private set

    private var tempPassword: String? = null

    /**
     * The value is positive at the fresh app start until the setup start screen is visited.
     */
    var hasCompletedInitialSetup = walletStorage.setupPreferences.getHasCompletedInitialSetup()
        private set

    private val _isLoggedIn = MutableLiveData<Boolean>(false)
    val isLoggedIn: LiveData<Boolean>
        get() = _isLoggedIn

    // The notice must be shown once per app start.
    private var isUnshieldingNoticeShown = false

    fun setHasShowRewards(id: Int, value: Boolean) {
        walletStorage.filterPreferences.setHasShowRewards(id, value)
    }

    fun getHasShowRewards(id: Int): Boolean {
        return  walletStorage.filterPreferences.getHasShowRewards(id)
    }

    fun setHasShowFinalizationRewards(id: Int, value: Boolean) {
        walletStorage.filterPreferences.setHasShowFinalizationRewards(id, value)
    }

    fun getHasShowFinalizationRewards(id: Int): Boolean {
        return  walletStorage.filterPreferences.getHasShowFinalizationRewards(id)
    }

    fun unshieldingNoticeShown() {
        isUnshieldingNoticeShown = true
    }

    fun isUnshieldingNoticeShown(): Boolean =
        isUnshieldingNoticeShown

    fun hasSetupPassword(passcodeUsed: Boolean = false) {
        _isLoggedIn.value = true
        walletStorage.setupPreferences.setHasSetupUser(true)
        App.appCore.authManager.setUsePassCode(passcodeUsed)
        hasSetupPassword = true
    }

    fun hasFinishedSetupPassword() {
        println("OOLEG here")
        Exception().printStackTrace()
        tempPassword = null
    }

    fun startedInitialSetup() {
        walletStorage.setupPreferences.setHasCompletedInitialSetup(false)
        hasCompletedInitialSetup = false
    }

    fun hasCompletedInitialSetup() {
        walletStorage.setupPreferences.setHasCompletedInitialSetup(true)
        hasCompletedInitialSetup = true
    }

    fun startPasswordSetup(password: String) {
        tempPassword = password
    }

    fun checkPassword(password: String): Boolean {
        return password.equals(tempPassword)
    }

    fun getPasswordToSetUp(): String? = tempPassword

    fun hasLoggedInUser() {
        _isLoggedIn.value = true
        resetLogoutTimeout()
    }

    fun resetLogoutTimeout() {
        if (_isLoggedIn.value!!) {
            inactivityCountDownTimer.cancel()
            inactivityCountDownTimer.start()
        }
    }

    var inactivityCountDownTimer =
        object : CountDownTimer(60 * 5 * 1000.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                _isLoggedIn.value = false
            }
        }

    fun getCurrentAuthSlot(): String {
        return appAuthPreferences.getCurrentAuthSlot()
    }

    fun setCurrentAuthSlot(resetBiometricKeyNameAppendix: String) {
        appAuthPreferences.setCurrentAuthSlot(resetBiometricKeyNameAppendix)
    }

    fun isAccountsBackupPossible(): Boolean {
        return !walletStorage.setupPreferences.hasEncryptedSeed()
    }

    fun isAccountsBackedUp(): Boolean {
        return walletStorage.setupPreferences.isAccountsBackedUp()
    }

    fun setAccountsBackedUp(value: Boolean) {
        return walletStorage.setupPreferences.setAccountsBackedUp(value)
    }

    fun addAccountsBackedUpListener(listener: Preferences.Listener) {
        walletStorage.setupPreferences.addAccountsBackedUpListener(listener)
    }

    fun removeAccountsBackedUpListener(listener: Preferences.Listener) {
        walletStorage.setupPreferences.removeListener(listener)
    }
}
