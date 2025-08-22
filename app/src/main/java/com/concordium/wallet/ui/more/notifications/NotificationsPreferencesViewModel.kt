package com.concordium.wallet.ui.more.notifications

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.notifications.UpdateNotificationsSubscriptionUseCase
import com.concordium.wallet.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val walletNotificationsPreferences =
        App.appCore.session.walletStorage.notificationsPreferences
    private val updateNotificationsSubscriptionUseCase =
        UpdateNotificationsSubscriptionUseCase()

    private val _areCcdTxNotificationsEnabledFlow = MutableStateFlow(false)
    val areCcdTxNotificationsEnabledFlow = _areCcdTxNotificationsEnabledFlow.asStateFlow()

    private val _areCis2TxNotificationsEnabledFlow = MutableStateFlow(false)
    val areCis2TxNotificationsEnabledFlow = _areCis2TxNotificationsEnabledFlow.asStateFlow()

    private val _arePltTxNotificationsEnabledFlow = MutableStateFlow(false)
    val arePltTxNotificationsEnabledFlow = _arePltTxNotificationsEnabledFlow.asStateFlow()

    private val _requestNotificationPermissionFlow = MutableStateFlow(Event(false))
    val requestNotificationPermissionFlow = _requestNotificationPermissionFlow.asStateFlow()

    private val _isCcdSwitchEnabledFlow = MutableStateFlow(true)
    val isCcdSwitchEnabledFlow = _isCcdSwitchEnabledFlow.asStateFlow()

    private val _isCis2SwitchEnabledFlow = MutableStateFlow(true)
    val isCis2SwitchEnabledFlow = _isCis2SwitchEnabledFlow.asStateFlow()

    private val _isPltSwitchEnabledFlow = MutableStateFlow(true)
    val isPltSwitchEnabledFlow = _isPltSwitchEnabledFlow.asStateFlow()

    init {
        _areCcdTxNotificationsEnabledFlow.value =
            walletNotificationsPreferences.areCcdTxNotificationsEnabled
        _areCis2TxNotificationsEnabledFlow.value =
            walletNotificationsPreferences.areCis2TxNotificationsEnabled
        _arePltTxNotificationsEnabledFlow.value =
            walletNotificationsPreferences.arePltTxNotificationsEnabled
    }

    fun onTxClicked(switchType: TxType) = viewModelScope.launch {
        _isCcdSwitchEnabledFlow.value = false
        _isCis2SwitchEnabledFlow.value = false
        _isPltSwitchEnabledFlow.value = false

        when (switchType) {
            is TxType.CCD -> onCcdTxClicked()
            is TxType.CIS2 -> onCis2TxClicked()
            is TxType.PLT -> onPltTxClicked()
        }
        _isCcdSwitchEnabledFlow.value = true
        _isCis2SwitchEnabledFlow.value = true
        _isPltSwitchEnabledFlow.value = true
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
        isPltTxEnabled: Boolean = walletNotificationsPreferences.arePltTxNotificationsEnabled
    ): Boolean {
        Log.d(
            "updating_subscriptions:" +
                    "\nisCcdTxEnabled=$isCcdTxEnabled," +
                    "\nisCis2TxEnabled=$isCis2TxEnabled"
        )
        return updateNotificationsSubscriptionUseCase(
            isCcdTxEnabled = isCcdTxEnabled,
            isCis2TxEnabled = isCis2TxEnabled,
            isPltTxEnabled = isPltTxEnabled
        )
    }

    private suspend fun onCcdTxClicked() {
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
            walletNotificationsPreferences.areCcdTxNotificationsEnabled =
                areCcdTxNotificationsEnabled
        }
    }

    private suspend fun onCis2TxClicked() {
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
            walletNotificationsPreferences.areCis2TxNotificationsEnabled =
                areCis2TxNotificationsEnabled
        }
    }

    private suspend fun onPltTxClicked() {
        val arePltTxNotificationsEnabled = _arePltTxNotificationsEnabledFlow.value.not()
        if (arePltTxNotificationsEnabled) {
            requestNotificationPermissionIfNeeded()
        }
        val success = updateNotificationsSubscription(
            isPltTxEnabled = arePltTxNotificationsEnabled
        )
        Log.d("success: $success")
        if (success) {
            _arePltTxNotificationsEnabledFlow.value = arePltTxNotificationsEnabled
            walletNotificationsPreferences.arePltTxNotificationsEnabled =
                arePltTxNotificationsEnabled
        }
    }

    sealed interface TxType {
        object CCD : TxType
        object CIS2 : TxType
        object PLT : TxType
    }
}
