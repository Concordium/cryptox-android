package com.concordium.wallet.ui.more.tracking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App

class TrackingPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val appTrackingPreferences = App.appCore.appTrackingPreferences

    private val _isTrackingEnabledLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val isTrackingEnabledLiveData: LiveData<Boolean> = _isTrackingEnabledLiveData

    init {
        _isTrackingEnabledLiveData.value = appTrackingPreferences.isTrackingEnabled
    }

    fun onAllowClicked() {
        val isTrackingEnabled = _isTrackingEnabledLiveData.value != true
        _isTrackingEnabledLiveData.value = isTrackingEnabled
        appTrackingPreferences.isTrackingEnabled = isTrackingEnabled

        appTrackingPreferences.hasDecidedOnPermission = true
    }
}
