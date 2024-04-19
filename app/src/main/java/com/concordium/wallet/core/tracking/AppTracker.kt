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

    fun trackWelcomeCheckBoxChecked() =
        TrackHelper.track()
            .interaction("Welcome screen check box", INTERACTION_CHECKED)
            .with(tracker)

    fun trackWelcomeGetStartedClicked() =
        TrackHelper.track()
            .interaction("Welcome screen 'Get started' button", INTERACTION_CLICKED)
            .with(tracker)

    fun trackHomeActivateAccountClicked() =
        TrackHelper.track()
            .interaction("Home screen 'Activate account' button", INTERACTION_CLICKED)
            .with(tracker)

    private companion object {
        private const val INTERACTION_CHECKED = "checked"
        private const val INTERACTION_CLICKED = "clicked"
    }
}
