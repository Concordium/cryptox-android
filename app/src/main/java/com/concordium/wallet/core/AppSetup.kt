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
    val isHasShowReviewDialogAfterReceiveFunds: Boolean
        get() = appSetupPreferences.getHasShowReviewDialogAfterReceiveFunds()
    val isHasShowReviewDialogAfterSendFunds: Boolean
        get() = appSetupPreferences.getHasShowReviewDialogAfterSendFunds()
    val isHasShowReviewDialogAfterEarnSetup: Boolean
        get() = appSetupPreferences.getHasShowReviewDialogAfterEarnSetup()
    val isHasShowReviewDialogAfterHalfYear: Boolean
        get() = appSetupPreferences.getHasShowReviewDialogAfterHalfYear()
    val showReviewDialogTime: Long
        get() = appSetupPreferences.getShowReviewDialogTime()
    val isHasAcceptedOnRampDisclaimer: Boolean
        get() = appSetupPreferences.getHasAcceptedOnRampDisclaimer()
    val showBalanceInEur: Boolean
        get() = appSetupPreferences.getShowBalanceInEur()

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

    fun setHasShowReviewDialogAfterReceiveFunds(value: Boolean) {
        appSetupPreferences.setHasShowReviewDialogAfterReceiveFunds(value)
    }

    fun setHasShowReviewDialogAfterSendFunds(value: Boolean) {
        appSetupPreferences.setHasShowReviewDialogAfterSendFunds(value)
    }

    fun setHasShowReviewDialogAfterEarnSetup(value: Boolean) {
        appSetupPreferences.setHasShowReviewDialogAfterEarnSetup(value)
    }

    fun setHasShowReviewDialogAfterHalfYear(value: Boolean) {
        appSetupPreferences.setHasShowReviewDialogAfterHalfYear(value)
    }

    fun setShowReviewDialogSnapshotTime() {
        appSetupPreferences.setShowReviewDialogSnapshotTime()
    }

    fun setHasAcceptedOnRampDisclaimer(value: Boolean) {
        appSetupPreferences.setHasAcceptedOnRampDisclaimer(value)
    }

    fun setShowBalanceInEur(value: Boolean) {
        appSetupPreferences.setShowBalanceInEur(value)
    }
}
