package com.concordium.wallet.core.tracking

import android.content.Context
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.DownloadTracker
import org.matomo.sdk.extra.TrackHelper

class MatomoAppTracker(private val tracker: Tracker) : AppTracker {
    override fun installation(context: Context) =
        track()
            .download()
            .identifier(DownloadTracker.Extra.ApkChecksum(context))
            .with(tracker)

    override fun welcomeScreen() =
        trackScreenAndImpression(SCREEN_WELCOME)

    override fun welcomeCheckBoxChecked() =
        trackInteraction(SCREEN_WELCOME, "Check box", INTERACTION_CHECKED)

    override fun welcomeGetStartedClicked() =
        trackInteraction(SCREEN_WELCOME, "Get started", INTERACTION_CLICKED)

    override fun welcomeHomeScreen() =
        trackScreenAndImpression(SCREEN_WELCOME_HOME)

    override fun welcomeHomeActivateAccountClicked() =
        trackInteraction(SCREEN_WELCOME_HOME, "Activate account", INTERACTION_CLICKED)

    override fun welcomeActivateAccountDialog() =
        trackScreenAndImpression(DIALOG_ACTIVATE_ACCOUNT)

    override fun welcomeActivateAccountDialogCreateClicked() =
        trackInteraction(DIALOG_ACTIVATE_ACCOUNT, "Create wallet", INTERACTION_CLICKED)

    override fun welcomeActivateAccountDialogImportClicked() =
        trackInteraction(DIALOG_ACTIVATE_ACCOUNT, "Import wallet", INTERACTION_CLICKED)

    override fun welcomePasscodeScreen() =
        trackScreenAndImpression(SCREEN_WELCOME_PASSCODE)

    override fun welcomePasscodeEntered() =
        trackInteraction(SCREEN_WELCOME_PASSCODE, "6-digit passcode", INTERACTION_ENTERED)

    override fun welcomePasscodeConfirmationEntered() =
        trackInteraction(
            SCREEN_WELCOME_PASSCODE,
            "6-digit passcode confirmation",
            INTERACTION_ENTERED
        )

    override fun welcomePasscodeBiometricsDialog() =
        trackScreenAndImpression(DIALOG_WELCOME_PASSCODE_BIOMETRICS)

    override fun welcomePasscodeBiometricsAccepted() =
        trackInteraction(DIALOG_WELCOME_PASSCODE_BIOMETRICS, "Enable", INTERACTION_CLICKED)

    override fun welcomePasscodeBiometricsRejected() =
        trackInteraction(DIALOG_WELCOME_PASSCODE_BIOMETRICS, "Later", INTERACTION_CLICKED)

    override fun welcomePhrase() =
        trackScreenAndImpression(SCREEN_WELCOME_PHRASE)

    override fun welcomePhraseCopyClicked() =
        trackInteraction(SCREEN_WELCOME_PHRASE, "Copy to clipboard", INTERACTION_CLICKED)

    override fun welcomePhraseCheckboxBoxChecked() =
        trackInteraction(SCREEN_WELCOME_PHRASE, "Check box", INTERACTION_CHECKED)

    override fun identityVerificationScreen(provider: String) {
        track()
            .screen(SCREEN_ID_VERIFICATION)
            .dimension(0, provider)
            .safelyWith(tracker)
    }

    override fun identityVerificationResultScreen() =
        trackScreenAndImpression(SCREEN_ID_VERIFICATION_RESULT)

    override fun identityVerificationResultApprovedDialog() =
        trackScreenAndImpression(DIALOG_ID_VERIFICATION_APPROVED)

    override fun identityVerificationResultCreateAccountClicked() =
        trackInteraction(SCREEN_ID_VERIFICATION_RESULT, "Create account", INTERACTION_CLICKED)

    override fun homeScreen() =
        trackScreenAndImpression(SCREEN_HOME)

    private fun track() =
        TrackHelper.track()

    private fun TrackHelper.screen(screen: Screen) =
        screen(screen.path).title(screen.title)

    private fun TrackHelper.interaction(screen: Screen, piece: String, interaction: String) =
        interaction(screen.title, interaction).piece(piece)

    private fun trackScreenAndImpression(screen: Screen) {
        track()
            .screen(screen)
            .safelyWith(tracker)
        track()
            .impression(screen.title)
            .safelyWith(tracker)
    }

    private fun trackInteraction(screen: Screen, piece: String, interaction: String) {
        track()
            .interaction(screen, piece, interaction)
            .safelyWith(tracker)
    }

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
