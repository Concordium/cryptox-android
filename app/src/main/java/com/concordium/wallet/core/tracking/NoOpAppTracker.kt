package com.concordium.wallet.core.tracking

import android.content.Context
import com.concordium.wallet.util.Log

class NoOpAppTracker : AppTracker {
    override fun installation(context: Context) = Log.d("No op")
    override fun welcomeScreen() = Log.d("No op")
    override fun welcomeCheckBoxChecked() = Log.d("No op")
    override fun welcomeGetStartedClicked() = Log.d("No op")
    override fun welcomeHomeScreen() = Log.d("No op")
    override fun welcomeHomeActivateAccountClicked() = Log.d("No op")
    override fun welcomeActivateAccountDialog() = Log.d("No op")
    override fun welcomeActivateAccountDialogCreateClicked() = Log.d("No op")
    override fun welcomeActivateAccountDialogImportClicked() = Log.d("No op")
    override fun welcomePasscodeScreen() = Log.d("No op")
    override fun welcomePasscodeEntered() = Log.d("No op")
    override fun welcomePasscodeConfirmationEntered() = Log.d("No op")
    override fun welcomePasscodeBiometricsDialog() = Log.d("No op")
    override fun welcomePasscodeBiometricsAccepted() = Log.d("No op")
    override fun welcomePasscodeBiometricsRejected() = Log.d("No op")
    override fun welcomePhrase() = Log.d("No op")
    override fun welcomePhraseCopyClicked() = Log.d("No op")
    override fun welcomePhraseCheckboxBoxChecked() = Log.d("No op")
    override fun identityVerificationScreen(provider: String) = Log.d("No op")
    override fun identityVerificationResultScreen() = Log.d("No op")
    override fun identityVerificationResultApprovedDialog() = Log.d("No op")
    override fun identityVerificationResultCreateAccountClicked() = Log.d("No op")
    override fun homeScreen() = Log.d("No op")
}
