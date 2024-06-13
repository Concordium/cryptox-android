package com.concordium.wallet.core.tracking

import android.content.Context

interface AppTracker {
    fun installation(context: Context)
    fun welcomeScreen()
    fun welcomeCheckBoxChecked()
    fun welcomeGetStartedClicked()
    fun welcomeHomeScreen()
    fun welcomeHomeActivateAccountClicked()
    fun welcomeActivateAccountDialog()
    fun welcomeActivateAccountDialogCreateClicked()
    fun welcomeActivateAccountDialogImportClicked()
    fun welcomePasscodeScreen()
    fun welcomePasscodeEntered()
    fun welcomePasscodeConfirmationEntered()
    fun welcomePasscodeBiometricsDialog()
    fun welcomePasscodeBiometricsAccepted()
    fun welcomePasscodeBiometricsRejected()
    fun welcomePhrase()
    fun welcomePhraseCopyClicked()
    fun welcomePhraseCheckboxBoxChecked()
    fun identityVerificationScreen(provider: String)
    fun identityVerificationResultScreen()
    fun identityVerificationResultApprovedDialog()
    fun identityVerificationResultCreateAccountClicked()
    fun homeScreen()
}
