package com.concordium.wallet.core

import com.concordium.wallet.data.preferences.AppSetupPreferences

class AppSetup(
    private val appSetupPreferences: AppSetupPreferences,
    private val getSession: () -> Session,
) {
    var auth = AppAuth(appSetupPreferences)
        private set
    private var oldAuth = auth
    var authSetupPassword: String? = null
        private set
    var authResetMasterKey: ByteArray? = null
        private set
    val isInitialSetupCompleted: Boolean
        get() = appSetupPreferences.getHasCompletedInitialSetup()
    val isAuthSetupCompleted: Boolean
        get() = auth.isPasswordAuthInitialized()

    fun finishInitialSetup() {
        appSetupPreferences.setHasCompletedInitialSetup(true)
    }

    fun beginAuthSetup(password: String) {
        authSetupPassword = password
    }

    fun finishAuthSetup() {
        authSetupPassword = null
        getSession().setUserLoggedIn()
    }

    /**
     * Allows manipulating the [auth] without making the change permanent
     * until [commitAuthReset] is called. In case of failure, call [cancelAuthReset].
     *
     * @param decryptedMasterKey to be used for during the reset, see [authResetMasterKey]
     */
    fun beginAuthReset(decryptedMasterKey: ByteArray) {
        // Back up the current auth manager and replace it
        // with the one with an alternative slot.
        oldAuth = auth
        auth = AppAuth(
            appSetupPreferences = appSetupPreferences,
            slot = System.currentTimeMillis().toString()
        )
        // Store the decrypted master key in memory
        // to re-init the auth with it later.
        this.authResetMasterKey = decryptedMasterKey
    }

    fun commitAuthReset() {
        authResetMasterKey = null
        // Save the current (alternative) slot and continue using it.
        auth.commitCurrentSlot()
        finishAuthSetup()
    }

    fun cancelAuthReset() {
        authResetMasterKey = null
        // Restore the auth manager discarding the alternative slot.
        auth = oldAuth
        finishAuthSetup()
    }
}
