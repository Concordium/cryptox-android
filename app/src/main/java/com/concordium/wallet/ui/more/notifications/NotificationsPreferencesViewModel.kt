package com.concordium.wallet.ui.more.notifications

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.preferences.NotificationsPreferences
import com.concordium.wallet.util.Log

class NotificationsPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val notificationsPreferences = NotificationsPreferences(application)

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

    init {
        _areCcdTxNotificationsEnabledLiveData.value =
            notificationsPreferences.areCcdTxNotificationsEnabled
        _areCis2TxNotificationsEnabledLiveData.value =
            notificationsPreferences.areCis2TxNotificationsEnabled
    }

    fun onCcdTxClicked() {
        val areCcdTxNotificationsEnabled = _areCcdTxNotificationsEnabledLiveData.value != true
        _areCcdTxNotificationsEnabledLiveData.value = areCcdTxNotificationsEnabled
        notificationsPreferences.areCcdTxNotificationsEnabled = areCcdTxNotificationsEnabled
        if (areCcdTxNotificationsEnabled) {
            requestNotificationPermissionIfNeeded()
        }
    }

    fun onCis2TxClicked() {
        val areCis2TxNotificationsEnabled = _areCis2TxNotificationsEnabledLiveData.value != true
        _areCis2TxNotificationsEnabledLiveData.value = areCis2TxNotificationsEnabled
        notificationsPreferences.areCis2TxNotificationsEnabled = areCis2TxNotificationsEnabled
        if (areCis2TxNotificationsEnabled) {
            requestNotificationPermissionIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(
                "requesting_permission"
            )

            _requestNotificationPermissionLiveData.postValue(Event(true))
        } else {
            Log.d(
                "no_need_to_request"
            )
        }
    }
}
