package com.concordium.wallet.ui.more.notifications

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.notifications.UpdateNotificationsSubscriptionUseCase
import com.concordium.wallet.data.preferences.WalletNotificationsPreferences
import com.concordium.wallet.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val walletNotificationsPreferences = WalletNotificationsPreferences(application)
    private val updateNotificationsSubscriptionUseCase =
        UpdateNotificationsSubscriptionUseCase(application)

    private val _areCcdTxNotificationsEnabledFlow = MutableStateFlow(false)
    val areCcdTxNotificationsEnabledFlow = _areCcdTxNotificationsEnabledFlow.asStateFlow()

    private val _areCis2TxNotificationsEnabledFlow = MutableStateFlow(false)
    val areCis2TxNotificationsEnabledFlow = _areCis2TxNotificationsEnabledFlow.asStateFlow()

    private val _requestNotificationPermissionFlow = MutableStateFlow(Event(false))
    val requestNotificationPermissionFlow = _requestNotificationPermissionFlow.asStateFlow()

    private val _isCcdSwitchEnabledFlow = MutableStateFlow(true)
    val isCcdSwitchEnabledFlow = _isCcdSwitchEnabledFlow.asStateFlow()

    private val _isCis2SwitchEnabledFlow = MutableStateFlow(true)
    val isCis2SwitchEnabledFlow = _isCis2SwitchEnabledFlow.asStateFlow()

    init {
        _areCcdTxNotificationsEnabledFlow.value =
            walletNotificationsPreferences.areCcdTxNotificationsEnabled
        _areCis2TxNotificationsEnabledFlow.value =
            walletNotificationsPreferences.areCis2TxNotificationsEnabled
    }

    fun onCcdTxClicked() = viewModelScope.launch {
        _isCcdSwitchEnabledFlow.value = false
        _isCis2SwitchEnabledFlow.value = false

        val areCcdTxNotificationsEnabled = _areCcdTxNotificationsEnabledFlow.value.not()

        if (areCcdTxNotificationsEnabled) {
            requestNotificationPermissionIfNeeded()
        }

        val success = updateNotificationsSubscription(
            isCcdTxEnabled = areCcdTxNotificationsEnabled,
        )
        Log.d("success: $success")
        if (success) {
            _areCcdTxNotificationsEnabledFlow.value = areCcdTxNotificationsEnabled
            walletNotificationsPreferences.areCcdTxNotificationsEnabled = areCcdTxNotificationsEnabled
        }

        _isCcdSwitchEnabledFlow.value = true
        _isCis2SwitchEnabledFlow.value = true
    }

    fun onCis2TxClicked() = viewModelScope.launch {
        _isCcdSwitchEnabledFlow.value = false
        _isCis2SwitchEnabledFlow.value = false

        val areCis2TxNotificationsEnabled = _areCis2TxNotificationsEnabledFlow.value.not()

        if (areCis2TxNotificationsEnabled) {
            requestNotificationPermissionIfNeeded()
        }

        val success = updateNotificationsSubscription(
            isCis2TxEnabled = areCis2TxNotificationsEnabled,
        )
        Log.d("success: $success")
        if (success) {
            _areCis2TxNotificationsEnabledFlow.value = areCis2TxNotificationsEnabled
            walletNotificationsPreferences.areCis2TxNotificationsEnabled = areCis2TxNotificationsEnabled
        }

        _isCcdSwitchEnabledFlow.value = true
        _isCis2SwitchEnabledFlow.value = true
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("requesting_permission")
            _requestNotificationPermissionFlow.value = Event(true)
        } else {
            Log.d("no_need_to_request")
        }
    }

    private suspend fun updateNotificationsSubscription(
        isCcdTxEnabled: Boolean = walletNotificationsPreferences.areCcdTxNotificationsEnabled,
        isCis2TxEnabled: Boolean = walletNotificationsPreferences.areCis2TxNotificationsEnabled,
    ): Boolean {
        Log.d(
            "updating_subscriptions:" +
                    "\nisCcdTxEnabled=$isCcdTxEnabled," +
                    "\nisCis2TxEnabled=$isCis2TxEnabled"
        )
        return updateNotificationsSubscriptionUseCase(
            isCcdTxEnabled = isCcdTxEnabled,
            isCis2TxEnabled = isCis2TxEnabled,
        )
    }
}
