package com.concordium.wallet.core.tracking

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAppTracker(
    private val analytics: FirebaseAnalytics,
): AppTracker {

    override fun installation(context: Context) {
        // Not needed as 'first_open' event is collected automatically.
    }

    override fun welcomeScreen() =
        screenVisit(SCREEN_WELCOME)

    override fun welcomeTermAndConditionsCheckBoxChecked() =
        contentSelection(
            contentName = "Terms and Conditions check box",
            contentType = CONTENT_TYPE_CHECK_BOX,
        )

    override fun welcomeActivityTrackingCheckBoxChecked() =
        contentSelection(
            contentName = "Activity Tracking check box",
            contentType = CONTENT_TYPE_CHECK_BOX,
        )

    override fun welcomeGetStartedClicked() =
        contentSelection(
            contentName = "Get started",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun welcomeActivateAccountDialog() =
        screenVisit(DIALOG_ACTIVATE_ACCOUNT)

    override fun welcomeActivateAccountDialogCreateClicked() =
        contentSelection(
            contentName = "Create wallet",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun welcomeActivateAccountDialogImportClicked() =
        contentSelection(
            contentName = "Import wallet",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun welcomePasscodeScreen() =
        screenVisit(SCREEN_WELCOME_PASSCODE)

    override fun welcomePasscodeEntered() =
        input(
            contentName = "6-digit passcode",
        )

    override fun welcomePasscodeConfirmationEntered() =
        input(
            contentName = "6-digit passcode confirmation",
        )

    override fun welcomePasscodeBiometricsDialog() =
        screenVisit(DIALOG_WELCOME_PASSCODE_BIOMETRICS)

    override fun welcomePasscodeBiometricsAccepted() =
        contentSelection(
            contentName = "Biometrics enable",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun welcomePasscodeBiometricsRejected() =
        contentSelection(
            contentName = "Biometrics later",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun seedPhraseScreen() =
        screenVisit(SCREEN_SAVE_PHRASE)

    override fun seedPhraseCopyClicked() =
        contentSelection(
            contentName = "Phrase copy to clipboard",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun seedPhraseCheckboxBoxChecked() =
        contentSelection(
            contentName = "I backed up my seed phrase",
            contentType = CONTENT_TYPE_CHECK_BOX,
        )

    override fun seedPhraseContinueClicked() =
        contentSelection(
            contentName = "Phrase continue",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun identityVerificationProvidersListScreen() =
        screenVisit(SCREEN_ID)

    override fun identityVerificationScreen(provider: String) =
        screenVisit(
            screen = SCREEN_ID_VERIFICATION,
            extraParams = bundleOf(
                "provider" to provider,
            )
        )

    override fun identityVerificationResultScreen() =
        screenVisit(SCREEN_ID_VERIFICATION_RESULT)

    override fun identityVerificationResultApprovedDialog() =
        screenVisit(DIALOG_ID_VERIFICATION_APPROVED)

    override fun identityVerificationResultCreateAccountClicked() =
        contentSelection(
            contentName = "Identity verification create account",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun homeScreen() =
        screenVisit(SCREEN_HOME)

    override fun homeSaveSeedPhraseClicked() =
        contentSelection(
            contentName = "Home save seed phrase",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun homeIdentityVerificationClicked() =
        contentSelection(
            contentName = "Home verify identity",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun homeIdentityVerificationStateChanged(state: String) =
        analytics.logEvent(
            "home_id_verification_state_change",
            bundleOf(
                "state" to state,
            )
        )

    override fun homeCreateAccountClicked() =
        contentSelection(
            contentName = "Home create account",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun homeOnRampScreen() =
        screenVisit(SCREEN_ONRAMP)

    override fun homeOnRampSiteClicked(siteName: String) =
        contentSelection(
            contentName = "Onramp $siteName",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun homeOnRampBannerClicked() =
        contentSelection(
            contentName = "Onramp banner",
            contentType = CONTENT_TYPE_BANNER,
        )

    override fun homeUnlockFeatureDialog() =
        screenVisit(DIALOG_UNLOCK_FEATURE)

    override fun homeTotalBalanceClicked() =
        contentSelection(
            contentName = "Wallet total balance",
            contentType = CONTENT_TYPE_LABEL,
        )

    override fun aboutScreen() =
        screenVisit(SCREEN_ABOUT)

    override fun aboutScreenLinkClicked(url: String) =
        contentSelection(
            contentName = "About: $url",
            contentType = CONTENT_TYPE_LINK,
        )

    override fun homeNewsScreen() =
        screenVisit(SCREEN_HOME_NEWS)

    private fun screenVisit(
        screen: Screen,
        extraParams: Bundle? = null,
    ) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screen.title)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screen.path)
                extraParams?.also(::putAll)
            }
        )
    }

    private fun contentSelection(
        contentName: String,
        contentType: String,
    ) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SELECT_CONTENT,
            bundleOf(
                FirebaseAnalytics.Param.ITEM_NAME to contentName,
                FirebaseAnalytics.Param.CONTENT_TYPE to contentType,
            )
        )
    }

    private fun input(
        contentName: String,
    ) {
        analytics.logEvent(
            "enter_input",
            bundleOf(
                FirebaseAnalytics.Param.ITEM_NAME to contentName,
            )
        )
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
        private val SCREEN_WELCOME_PASSCODE = Screen(
            title = "Welcome: Passcode",
            path = "/welcome/passcode_setup",
        )
        private val SCREEN_SAVE_PHRASE = Screen(
            title = "Home: Phrase",
            path = "/home/phrase",
        )
        private val SCREEN_ID = Screen(
            title = "ID Screen",
            path = "/id_providers",
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
            path = "/home"
        )
        private val SCREEN_ONRAMP = Screen(
            title = "Home: OnRamp",
            path = "/home/onramp"
        )
        private val SCREEN_ABOUT = Screen(
            title = "About",
            path = "/about"
        )
        private val SCREEN_HOME_NEWS = Screen(
            title = "Home: News",
            path = "/home/news"
        )

        private val DIALOG_ACTIVATE_ACCOUNT = Screen(
            title = "Welcome: Activate account dialog",
            path = SCREEN_WELCOME.path + "#activate_account",
        )
        private val DIALOG_WELCOME_PASSCODE_BIOMETRICS = Screen(
            title = "Welcome: Passcode: Biometrics dialog",
            path = SCREEN_WELCOME.path + "#biometrics",
        )
        private val DIALOG_ID_VERIFICATION_APPROVED = Screen(
            title = "ID Verification approved",
            path = SCREEN_ID_VERIFICATION_RESULT.path + "#approved",
        )
        private val DIALOG_UNLOCK_FEATURE = Screen(
            title = "Home: Unlock feature dialog",
            path = SCREEN_HOME.path + "#unlock_feature"
        )

        private const val CONTENT_TYPE_CHECK_BOX = "checkbox"
        private const val CONTENT_TYPE_BUTTON = "button"
        private const val CONTENT_TYPE_BANNER = "banner"
        private const val CONTENT_TYPE_LABEL = "label"
        private const val CONTENT_TYPE_LINK = "link"
    }
}
