package com.concordium.wallet.ui.identity.identitiesoverview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.App
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Identity

class IdentitiesOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val identityRepository =
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    val identityListLiveData: LiveData<List<Identity>> =
        identityRepository.allIdentities

    fun initialize() {
    }
}
