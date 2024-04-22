package com.concordium.wallet.core.tracking

import android.content.Context
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.DownloadTracker
import org.matomo.sdk.extra.TrackHelper

class AppTracker(private val tracker: Tracker) {
    fun trackInstall(context: Context) =
        TrackHelper.track()
            .download()
            .identifier(DownloadTracker.Extra.ApkChecksum(context))
            .with(tracker)

    fun trackWelcomeScreen() =
        TrackHelper.track()
            .screen("/welcome")
            .title(SCREEN_WELCOME)
            .safelyWith(tracker)

    fun trackWelcomeCheckBoxChecked() =
        TrackHelper.track()
            .interaction(SCREEN_WELCOME, INTERACTION_CHECKED)
            .piece("Check box")
            .safelyWith(tracker)

    fun trackWelcomeGetStartedClicked() =
        TrackHelper.track()
            .interaction(SCREEN_WELCOME, INTERACTION_CLICKED)
            .piece("Get started")
            .safelyWith(tracker)

    fun trackWelcomeHomeScreen() =
        TrackHelper.track()
            .screen("/welcome/home")
            .title(SCREEN_WELCOME_HOME)
            .safelyWith(tracker)

    fun trackWelcomeHomeActivateAccountClicked() =
        TrackHelper.track()
            .interaction(SCREEN_WELCOME, INTERACTION_CLICKED)
            .piece("Activate account")
            .safelyWith(tracker)

    private companion object {
        private const val SCREEN_WELCOME = "Welcome"
        private const val SCREEN_WELCOME_HOME = "Welcome: Home"

        private const val INTERACTION_CHECKED = "checked"
        private const val INTERACTION_CLICKED = "clicked"
    }
}
