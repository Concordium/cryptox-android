package com.concordium.wallet.core.authentication

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.data.preferences.AppAuthPreferences
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.preferences.WalletSetupPreferences
import com.concordium.wallet.data.preferences.WalletFilterPreferences

class Session {

    private var appAuthPreferences: AppAuthPreferences
    private var walletSetupPreferences: WalletSetupPreferences
    private var walletFilterPreferences: WalletFilterPreferences

    var hasSetupPassword = false
        private set

    private var tempPassword: String? = null

    /**
     * The value is positive at the fresh app start until the setup start screen is visited.
     */
    var hasCompletedInitialSetup = true
        private set

    private val _isLoggedIn = MutableLiveData<Boolean>(false)
    val isLoggedIn: LiveData<Boolean>
        get() = _isLoggedIn

    // The notice must be shown once per app start.
    private var isUnshieldingNoticeShown = false

    constructor(context: Context) {
        appAuthPreferences = AppAuthPreferences(context)
        walletSetupPreferences = WalletSetupPreferences(context)
        hasSetupPassword = walletSetupPreferences.getHasSetupUser()
        hasCompletedInitialSetup = walletSetupPreferences.getHasCompletedInitialSetup()
        walletFilterPreferences = WalletFilterPreferences(context)
    }

    fun setHasShowRewards(id: Int, value: Boolean) {
        walletFilterPreferences.setHasShowRewards(id, value)
    }

    fun getHasShowRewards(id: Int): Boolean {
        return walletFilterPreferences.getHasShowRewards(id)
    }

    fun setHasShowFinalizationRewards(id: Int, value: Boolean) {
        walletFilterPreferences.setHasShowFinalizationRewards(id, value)
    }

    fun getHasShowFinalizationRewards(id: Int): Boolean {
        return walletFilterPreferences.getHasShowFinalizationRewards(id)
    }

    fun unshieldingNoticeShown() {
        isUnshieldingNoticeShown = true
    }

    fun isUnshieldingNoticeShown(): Boolean =
        isUnshieldingNoticeShown

    fun hasSetupPassword(passcodeUsed: Boolean = false) {
        _isLoggedIn.value = true
        walletSetupPreferences.setHasSetupUser(true)
        App.appCore.authManager.setUsePassCode(passcodeUsed)
        hasSetupPassword = true
    }

    fun hasFinishedSetupPassword() {
        println("OOLEG here")
        Exception().printStackTrace()
        tempPassword = null
    }

    fun startedInitialSetup() {
        walletSetupPreferences.setHasCompletedInitialSetup(false)
        hasCompletedInitialSetup = false
    }

    fun hasCompletedInitialSetup() {
        walletSetupPreferences.setHasCompletedInitialSetup(true)
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
        return !walletSetupPreferences.hasEncryptedSeed()
    }

    fun isAccountsBackedUp(): Boolean {
        return walletSetupPreferences.isAccountsBackedUp()
    }

    fun setAccountsBackedUp(value: Boolean) {
        return walletSetupPreferences.setAccountsBackedUp(value)
    }

    fun addAccountsBackedUpListener(listener: Preferences.Listener) {
        walletSetupPreferences.addAccountsBackedUpListener(listener)
    }

    fun removeAccountsBackedUpListener(listener: Preferences.Listener) {
        appAuthPreferences.removeListener(listener)
    }
}
