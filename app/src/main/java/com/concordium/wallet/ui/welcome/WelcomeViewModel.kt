package com.concordium.wallet.ui.welcome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.App

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {

    val shouldSetUpPassword: Boolean
        get() = !App.appCore.session.hasSetupPassword

    init {
        App.appCore.session.startedInitialSetup()
    }
}