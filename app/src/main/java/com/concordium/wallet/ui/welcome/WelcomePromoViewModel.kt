package com.concordium.wallet.ui.welcome

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.App
import com.concordium.wallet.data.preferences.NotificationPreferences
import java.math.BigInteger

class WelcomePromoViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationPreferences: NotificationPreferences by lazy {
        NotificationPreferences(application)
    }

    val shouldShowNotificationPermissionDialog: Boolean
        get() = IS_SENDING_ANY_NOTIFICATIONS
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !notificationPreferences.hasEverShownPermissionDialog
    val shouldSetUpPassword: Boolean
        get() = !App.appCore.session.hasSetupPassword
    val accountActivationReward = BigInteger("1000000000")
    val aiAssistantUrl = "https://www.concordium.com/contact"
    val videosUrl = "https://www.youtube.com/@ConcordiumNet/videos"

    fun initialize() {
        App.appCore.session.startedInitialSetup()
    }

    fun onNotificationPermissionDialogDismissed() {
        notificationPreferences.hasEverShownPermissionDialog = true
    }

    private companion object {
        private const val IS_SENDING_ANY_NOTIFICATIONS = false
    }
}
