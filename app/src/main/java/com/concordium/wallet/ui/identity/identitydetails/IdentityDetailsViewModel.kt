package com.concordium.wallet.ui.identity.identitydetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Identity
import kotlinx.coroutines.launch

class IdentityDetailsViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var identity: Identity
    private val identityRepository =
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    val identityChanged: MutableLiveData<Identity> by lazy { MutableLiveData<Identity>() }

    fun initialize(identity: Identity) {
        this.identity = identity
    }

    fun removeIdentity(identity: Identity) {
        viewModelScope.launch {
            identityRepository.delete(identity)
        }
    }

    fun changeIdentityName(name: String) {
        viewModelScope.launch {
            identity.name = name
            identityRepository.update(identity)
            identityChanged.value = identity
        }
    }
}
