package com.concordium.wallet.ui.welcome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.App
import com.concordium.wallet.data.preferences.TrackingPreferences
import java.math.BigInteger

class WelcomePromoViewModel(application: Application) : AndroidViewModel(application) {
    private val trackingPreferences: TrackingPreferences by lazy {
        TrackingPreferences(application)
    }

    val shouldShowTrackingPermissionDialog: Boolean
        get() = !trackingPreferences.hasDecidedOnPermission
    val shouldSetUpPassword: Boolean
        get() = !App.appCore.session.hasSetupPassword
    val accountActivationReward = BigInteger("1000000000")
    val aiAssistantUrl = "https://www.concordium.com/contact"
    val videosUrl = "https://www.youtube.com/watch?v=UQPPqXO7hZw&list=PLK_gvUmWN_G_x_63SZswz8ZEK-UqPPZjp&index=3"

    fun initialize() {
        App.appCore.session.startedInitialSetup()
    }

    private companion object {
        private const val IS_SENDING_ANY_NOTIFICATIONS = false
    }
}
