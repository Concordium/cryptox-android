package com.concordium.wallet.ui.welcome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.preferences.NotificationsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationsPreferences: NotificationsPreferences
    private val _isNotificationDialogEverShowed = MutableStateFlow(true)
    val isNotificationDialogEverShowed = _isNotificationDialogEverShowed.asStateFlow()

    val shouldSetUpPassword: Boolean
        get() = !App.appCore.session.hasSetupPassword

    init {
        App.appCore.session.startedInitialSetup()

        notificationsPreferences = NotificationsPreferences(application)
        viewModelScope.launch {
            // Show notifications permission if never shown
            _isNotificationDialogEverShowed.emit(notificationsPreferences.hasEverShownPermissionDialog)
        }
    }
}