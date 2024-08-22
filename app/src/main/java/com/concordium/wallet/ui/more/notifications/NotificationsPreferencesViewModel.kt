package com.concordium.wallet.ui.more.notifications

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.core.notifications.UpdateNotificationsSubscriptionUseCase
import com.concordium.wallet.data.preferences.NotificationsPreferences
import com.concordium.wallet.util.Log
import kotlinx.coroutines.launch

class NotificationsPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val notificationsPreferences = NotificationsPreferences(application)
    private val updateNotificationsSubscriptionUseCase =
        UpdateNotificationsSubscriptionUseCase(application)

    private val _areCcdTxNotificationsEnabledLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val areCcdTxNotificationsEnabledLiveData: LiveData<Boolean> =
        _areCcdTxNotificationsEnabledLiveData
    private val _areCis2TxNotificationsEnabledLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val areCis2TxNotificationsEnabledLiveData: LiveData<Boolean> =
        _areCis2TxNotificationsEnabledLiveData
    private val _requestNotificationPermissionLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData()
    val requestNotificationPermissionLiveData: LiveData<Event<Boolean>> =
        _requestNotificationPermissionLiveData

    private val _isCcdSwitchEnabledLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val isCcdSwitchEnabledLiveData: LiveData<Boolean> = _isCcdSwitchEnabledLiveData
    private val _isCis2SwitchEnabledLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val isCis2SwitchEnabledLiveData: LiveData<Boolean> = _isCis2SwitchEnabledLiveData

    init {
        _areCcdTxNotificationsEnabledLiveData.postValue(notificationsPreferences.areCcdTxNotificationsEnabled)
        _areCis2TxNotificationsEnabledLiveData.postValue(notificationsPreferences.areCis2TxNotificationsEnabled)
    }

    fun onCcdTxClicked() {
        _isCcdSwitchEnabledLiveData.postValue(false)
        val areCcdTxNotificationsEnabled = _areCcdTxNotificationsEnabledLiveData.value != true
        _areCcdTxNotificationsEnabledLiveData.postValue(!areCcdTxNotificationsEnabled)
        if (areCcdTxNotificationsEnabled) {
            requestNotificationPermissionIfNeeded()
        }
        viewModelScope.launch {
            val success = updateNotificationsSubscription(isCcdTxEnabled = areCcdTxNotificationsEnabled)
            Log.d("success: $success")
            if (success) {
                _areCcdTxNotificationsEnabledLiveData.postValue(areCcdTxNotificationsEnabled)
                notificationsPreferences.areCcdTxNotificationsEnabled = areCcdTxNotificationsEnabled
            } else {
                _areCcdTxNotificationsEnabledLiveData.postValue(!areCcdTxNotificationsEnabled)
            }
            _isCcdSwitchEnabledLiveData.postValue(true)
        }
    }

    fun onCis2TxClicked() {
        _isCis2SwitchEnabledLiveData.postValue(false)
        val areCis2TxNotificationsEnabled = _areCis2TxNotificationsEnabledLiveData.value != true
        _areCis2TxNotificationsEnabledLiveData.postValue(!areCis2TxNotificationsEnabled)
        if (areCis2TxNotificationsEnabled) {
            requestNotificationPermissionIfNeeded()
        }
        viewModelScope.launch {
            val success = updateNotificationsSubscription(isCis2TxEnabled = areCis2TxNotificationsEnabled)
            Log.d("success: $success")
            if (success) {
                _areCis2TxNotificationsEnabledLiveData.postValue(areCis2TxNotificationsEnabled)
                notificationsPreferences.areCis2TxNotificationsEnabled = areCis2TxNotificationsEnabled
            } else {
                _areCis2TxNotificationsEnabledLiveData.postValue(!areCis2TxNotificationsEnabled)
            }
            _isCis2SwitchEnabledLiveData.postValue(true)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("requesting_permission")
            _requestNotificationPermissionLiveData.postValue(Event(true))
        } else {
            Log.d("no_need_to_request")
        }
    }

    private suspend fun updateNotificationsSubscription(
        isCcdTxEnabled: Boolean? = null,
        isCis2TxEnabled: Boolean? = null
    ): Boolean {
        Log.d("updating_subscriptions")
        return updateNotificationsSubscriptionUseCase(
            isCcdTxEnabled = isCcdTxEnabled,
            isCis2TxEnabled = isCis2TxEnabled
        )
    }
}
