package com.concordium.wallet.ui.more.tracking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.data.preferences.TrackingPreferences

class TrackingPreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val trackingPreferences = TrackingPreferences(application)

    private val _isTrackingEnabledLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val isTrackingEnabledLiveData: LiveData<Boolean> = _isTrackingEnabledLiveData

    init {
        _isTrackingEnabledLiveData.value = trackingPreferences.isTrackingEnabled
    }

    fun onAllowClicked() {
        val isTrackingEnabled = _isTrackingEnabledLiveData.value != true
        _isTrackingEnabledLiveData.value = isTrackingEnabled
        trackingPreferences.isTrackingEnabled = isTrackingEnabled

        trackingPreferences.hasDecidedOnPermission = true
    }
}
