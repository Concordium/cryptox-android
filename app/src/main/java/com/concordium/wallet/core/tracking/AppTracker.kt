package com.concordium.wallet.core.tracking

import android.content.Context
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.DownloadTracker
import org.matomo.sdk.extra.TrackHelper

class AppTracker(private val tracker: Tracker) {
    fun installation(context: Context) =
        track()
            .download()
            .identifier(DownloadTracker.Extra.ApkChecksum(context))
            .with(tracker)

    fun welcomeScreen() =
        track()
            .screen(SCREEN_WELCOME)
            .safelyWith(tracker)

    fun welcomeCheckBoxChecked() =
        track()
            .interaction(SCREEN_WELCOME, "Check box", INTERACTION_CHECKED)
            .safelyWith(tracker)

    fun welcomeGetStartedClicked() =
        track()
            .interaction(SCREEN_WELCOME, "Get started", INTERACTION_CLICKED)
            .safelyWith(tracker)

    fun welcomeHomeScreen() =
        track()
            .screen(SCREEN_WELCOME_HOME)
            .safelyWith(tracker)

    fun welcomeHomeActivateAccountClicked() =
        track()
            .interaction(SCREEN_WELCOME_HOME, "Activate account", INTERACTION_CLICKED)
            .safelyWith(tracker)

    fun welcomeActivateAccountDialog() =
        track()
            .screen(DIALOG_ACTIVATE_ACCOUNT)
            .safelyWith(tracker)

    fun welcomeActivateAccountDialogCreateClicked() =
        track()
            .interaction(DIALOG_ACTIVATE_ACCOUNT, "Create wallet", INTERACTION_CLICKED)
            .safelyWith(tracker)

    fun welcomeActivateAccountDialogImportClicked() =
        track()
            .interaction(DIALOG_ACTIVATE_ACCOUNT, "Import wallet", INTERACTION_CLICKED)
            .safelyWith(tracker)

    fun welcomePasscodeScreen() =
        track()
            .screen(SCREEN_WELCOME_PASSCODE)
            .safelyWith(tracker)

    fun welcomePasscodeEntered() =
        track()
            .interaction(SCREEN_WELCOME_PASSCODE, "6-digit passcode", INTERACTION_ENTERED)
            .safelyWith(tracker)

    fun welcomePasscodeConfirmationEntered() =
        track()
            .interaction(
                SCREEN_WELCOME_PASSCODE,
                "6-digit passcode confirmation",
                INTERACTION_ENTERED
            )
            .safelyWith(tracker)

    fun welcomePasscodeBiometricsDialog() =
        track()
            .screen(DIALOG_WELCOME_PASSCODE_BIOMETRICS)
            .safelyWith(tracker)

    fun welcomePasscodeBiometricsAccepted() =
        track()
            .interaction(DIALOG_WELCOME_PASSCODE_BIOMETRICS, "Enable", INTERACTION_CLICKED)
            .safelyWith(tracker)

    fun welcomePasscodeBiometricsRejected() =
        track()
            .interaction(DIALOG_WELCOME_PASSCODE_BIOMETRICS, "Later", INTERACTION_CLICKED)
            .safelyWith(tracker)

    fun welcomePhrase() =
        track()
            .screen(SCREEN_WELCOME_PHRASE)
            .safelyWith(tracker)

    fun welcomePhraseCopyClicked() =
        track()
            .interaction(SCREEN_WELCOME_PHRASE, "Copy to clipboard", INTERACTION_CLICKED)
            .safelyWith(tracker)

    fun welcomePhraseCheckboxBoxChecked() =
        track()
            .interaction(SCREEN_WELCOME_PHRASE, "Check box", INTERACTION_CHECKED)
            .safelyWith(tracker)

    fun identityVerificationScreen(provider: String) =
        track()
            .screen(SCREEN_ID_VERIFICATION)
            .dimension(0, provider)
            .safelyWith(tracker)

    fun identityVerificationResultScreen() =
        track()
            .screen(SCREEN_ID_VERIFICATION_RESULT)
            .safelyWith(tracker)

    fun identityVerificationResultApprovedDialog() =
        track()
            .screen(DIALOG_ID_VERIFICATION_APPROVED)
            .safelyWith(tracker)

    fun identityVerificationResultCreateAccountClicked() =
        track()
            .interaction(SCREEN_ID_VERIFICATION_RESULT, "Create account", INTERACTION_CLICKED)
            .safelyWith(tracker)

    fun homeScreen() =
        track()
            .screen(SCREEN_HOME)
            .safelyWith(tracker)

    private fun track() =
        TrackHelper.track()

    private fun TrackHelper.screen(screen: Screen) =
        screen(screen.path).title(screen.title)

    private fun TrackHelper.interaction(screen: Screen, piece: String, interaction: String) =
        interaction(screen.title, interaction).piece(piece)

    private class Screen(
        val title: String,
        val path: String,
    )

    private companion object {
        private val SCREEN_WELCOME = Screen(
            title = "Welcome",
            path = "/welcome",
        )
        private val SCREEN_WELCOME_HOME = Screen(
            title = "Welcome: Home",
            path = "/welcome/home",
        )
        private val SCREEN_WELCOME_PASSCODE = Screen(
            title = "Welcome: Passcode",
            path = "/welcome/home/passcode_setup",
        )
        private val SCREEN_WELCOME_PHRASE = Screen(
            title = "Welcome: Phrase",
            path = "/welcome/home/phrase",
        )
        private val SCREEN_ID_VERIFICATION = Screen(
            title = "ID Verification",
            path = "/id_providers/verification",
        )
        private val SCREEN_ID_VERIFICATION_RESULT = Screen(
            title = "ID Verification result",
            path = "/id_providers/verification/result",
        )
        private val SCREEN_HOME = Screen(
            title = "Home",
            path = "/"
        )

        private val DIALOG_ACTIVATE_ACCOUNT = Screen(
            title = "Welcome: Activate account dialog",
            path = SCREEN_WELCOME_HOME.path + "#activate_account",
        )
        private val DIALOG_WELCOME_PASSCODE_BIOMETRICS = Screen(
            title = "Welcome: Passcode: Biometrics dialog",
            path = SCREEN_WELCOME_PASSCODE.path + "#biometrics",
        )
        private val DIALOG_ID_VERIFICATION_APPROVED = Screen(
            title = "ID Verification approved",
            path = SCREEN_ID_VERIFICATION_RESULT.path + "#approved",
        )

        private const val INTERACTION_CHECKED = "checked"
        private const val INTERACTION_CLICKED = "clicked"
        private const val INTERACTION_ENTERED = "entered"
    }
}
