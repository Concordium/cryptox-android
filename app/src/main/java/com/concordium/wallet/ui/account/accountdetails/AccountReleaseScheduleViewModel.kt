package com.concordium.wallet.ui.account.accountdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.model.Schedule
import com.concordium.wallet.data.room.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AccountReleaseScheduleViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var account: Account
    var isShielded: Boolean = false

    // Transaction state
    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val _errorLiveData = MutableLiveData<Event<Int>>()
    val errorLiveData: LiveData<Event<Int>>
        get() = _errorLiveData

    private val _finishLiveData = MutableLiveData<Event<Boolean>>()
    val finishLiveData: LiveData<Event<Boolean>>
        get() = _finishLiveData

    private var _scheduledReleasesLiveData = MutableLiveData<List<Schedule>>()
    val scheduledReleasesLiveData: LiveData<List<Schedule>>
        get() = _scheduledReleasesLiveData

    fun initialize(account: Account, isShielded: Boolean) {
        this.account = account
        this.isShielded = isShielded
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun populateScheduledReleaseList() {
        _waitingLiveData.value = true
        viewModelScope.launch(Dispatchers.IO) {
            _scheduledReleasesLiveData.postValue(account.releaseSchedule?.schedule ?: ArrayList())
            _waitingLiveData.postValue(false)
        }
    }
}
