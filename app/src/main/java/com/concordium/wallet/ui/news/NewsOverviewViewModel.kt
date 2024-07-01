package com.concordium.wallet.ui.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NewsOverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean> = _waitingLiveData
}
