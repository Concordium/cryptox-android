package com.concordium.wallet.ui.welcome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.preferences.WalletNotificationsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationsPreferences: WalletNotificationsPreferences =
        App.appCore.session.walletStorage.notificationsPreferences
    private val _isNotificationDialogEverShowed = MutableStateFlow(true)
    val isNotificationDialogEverShowed = _isNotificationDialogEverShowed.asStateFlow()

    val shouldSetUpPassword: Boolean
        get() = !App.appCore.setup.isAuthSetupCompleted

    init {
        viewModelScope.launch {
            // Show notifications permission if never shown
            _isNotificationDialogEverShowed.emit(notificationsPreferences.hasEverShownPermissionDialog)
        }
    }

    fun setNotificationDialogShowed() {
        viewModelScope.launch {
            _isNotificationDialogEverShowed.emit(true)
        }
    }
}
