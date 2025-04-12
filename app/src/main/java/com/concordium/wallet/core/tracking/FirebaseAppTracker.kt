package com.concordium.wallet.core.tracking

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAppTracker(
    private val analytics: FirebaseAnalytics,
) : AppTracker {

    /*
        ⚠️
        Screens must be tracked from RESUMED activities.
        If tracked in onCreate or in view model init,
        Firebase refuses to track.
     */

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

    override fun welcomeSetUpWalletDialog() =
        screenVisit(DIALOG_SET_UP_WALLET)

    override fun welcomeSetUpWalletDialogCreateClicked() =
        contentSelection(
            contentName = "Create wallet",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun welcomeSetUpWalletDialogImportClicked() =
        contentSelection(
            contentName = "Import wallet",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun passcodeScreen() =
        screenVisit(SCREEN_PASSCODE_SETUP)

    override fun passcodeSetupEntered() =
        input(
            contentName = "6-digit passcode",
        )

    override fun passcodeSetupConfirmationEntered() =
        input(
            contentName = "6-digit passcode confirmation",
        )

    override fun passcodeSetupBiometricsDialog() =
        screenVisit(DIALOG_PASSCODE_SETUP_BIOMETRICS)

    override fun passcodeSetupBiometricsAccepted() =
        contentSelection(
            contentName = "Biometrics enable",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun passcodeBiometricsRejected() =
        contentSelection(
            contentName = "Biometrics later",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun seedPhraseScreen() =
        screenVisit(SCREEN_PHRASE_SETUP)

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
        screenVisit(SCREEN_ID_PROVIDERS)

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

    override fun homeOnrampScreen() =
        screenVisit(SCREEN_ONRAMP)

    override fun homeOnrampSiteClicked(siteName: String) =
        contentSelection(
            contentName = "Onramp $siteName",
            contentType = CONTENT_TYPE_BUTTON,
        )

    override fun homeOnrampBannerClicked() =
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

    override fun discoverScreen() =
        screenVisit(SCREEN_DISCOVER)

    private fun screenVisit(
        screen: Screen,
        extraParams: Bundle? = null,
    ) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screen.title)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screen.slug)
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
        val slug: String,
    )

    private companion object {
        private val SCREEN_WELCOME = Screen(
            title = "Welcome",
            slug = "welcome",
        )
        private val SCREEN_PASSCODE_SETUP = Screen(
            title = "Passcode setup",
            slug = "passcode_setup",
        )
        private val SCREEN_PHRASE_SETUP = Screen(
            title = "Seed phrase setup",
            slug = "phrase_setup",
        )
        private val SCREEN_ID_PROVIDERS = Screen(
            title = "ID Providers",
            slug = "id_providers",
        )
        private val SCREEN_ID_VERIFICATION = Screen(
            title = "ID Verification",
            slug = "id_verification",
        )
        private val SCREEN_ID_VERIFICATION_RESULT = Screen(
            title = "ID Verification result",
            slug = "id_verification_result",
        )
        private val SCREEN_HOME = Screen(
            title = "Home",
            slug = "home"
        )
        private val SCREEN_ONRAMP = Screen(
            title = "Onramp",
            slug = "onramp"
        )
        private val SCREEN_ABOUT = Screen(
            title = "About",
            slug = "about"
        )
        private val SCREEN_DISCOVER = Screen(
            title = "Discover",
            slug = "discover"
        )

        private val DIALOG_SET_UP_WALLET = Screen(
            title = "Set up wallet dialog",
            slug = "set_up_wallet_dialog",
        )
        private val DIALOG_PASSCODE_SETUP_BIOMETRICS = Screen(
            title = "Welcome: Passcode: Biometrics dialog",
            slug = "passcode_setup_biometrics_dialog",
        )
        private val DIALOG_ID_VERIFICATION_APPROVED = Screen(
            title = "ID Verification approved",
            slug = "id_verification_approved_dialog",
        )
        private val DIALOG_UNLOCK_FEATURE = Screen(
            title = "Home: Unlock feature dialog",
            slug = "unlock_feature_dialog"
        )

        private const val CONTENT_TYPE_CHECK_BOX = "checkbox"
        private const val CONTENT_TYPE_BUTTON = "button"
        private const val CONTENT_TYPE_BANNER = "banner"
        private const val CONTENT_TYPE_LABEL = "label"
        private const val CONTENT_TYPE_LINK = "link"
    }
}
