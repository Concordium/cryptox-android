package com.concordium.wallet.ui.auth.setup.password

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.Event

class AuthSetupPasswordRepeatViewModel(application: Application) : AndroidViewModel(application) {

    private val _finishScreenLiveData = MutableLiveData<Event<Boolean>>()
    val finishScreenLiveData: LiveData<Event<Boolean>>
        get() = _finishScreenLiveData

    fun initialize() {
    }

    fun checkPassword(password: String) {
        val isEqual = App.appCore.setup.authSetupPassword == password
        _finishScreenLiveData.value = Event(isEqual)
    }
}
