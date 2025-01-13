package com.concordium.wallet.ui.airdrop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.backend.airdrop.AirDropRepository
import com.concordium.wallet.data.backend.airdrop.RegistrationRequest
import com.concordium.wallet.data.backend.airdrop.RegistrationResponse
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.extension.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AirDropViewModel(application: Application) : AndroidViewModel(application) {

    private val airDropRepo = AirDropRepository()
    private lateinit var apiUrl: String
    private var airdropId: Long? = null

    private val _viewState = MutableSharedFlow<AirDropState>()
    val viewState: SharedFlow<AirDropState> = _viewState.asSharedFlow()

    private val _errorLiveData: MutableLiveData<Event<String>> = MutableLiveData()
    fun onError(): LiveData<Event<String>> = _errorLiveData

    private val _walletsLiveData: MutableLiveData<Event<List<Account>>> = MutableLiveData()
    fun onWalletReady(): LiveData<Event<List<Account>>> = _walletsLiveData

    private val _doRegistrationResponseLiveData: MutableLiveData<Event<RegistrationResponse>> = MutableLiveData()
    fun doRegistrationResponse(): LiveData<Event<RegistrationResponse>> = _doRegistrationResponseLiveData

    private val accountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())

    fun processAction(action: AirDropAction) {
        when (action) {
            AirDropAction.GetWallets -> getWallets()
            is AirDropAction.SendRegistration -> sendRegistration(action.account)
            is AirDropAction.Init -> {
                apiUrl = action.apiUrl
                airdropId = action.airdropId
                viewModelScope.launch {
                    _viewState.emit(AirDropState.ConnectConfirm)
                }
            }
        }
    }

    private fun sendRegistration(acc: Account) {
        val registration = RegistrationRequest(
            airdropId = airdropId ?: 0,
            address = RegistrationRequest.Address(
                concordium = acc.address
            )
        )
        viewModelScope.launch {
            val resp = airDropRepo.doRegistration(registration, apiUrl)
            if (resp != null) {
                _doRegistrationResponseLiveData.postValue(Event(resp))
            } else {
                _errorLiveData.postValue(Event("Server error. Try again later"))
            }
        }
    }

    private fun getWallets() = viewModelScope.launch(Dispatchers.IO) {
        val wallets = accountRepository?.getAll() ?: emptyList()
        _walletsLiveData.postValue(Event(wallets))
        _viewState.emit(AirDropState.SelectWallet)
    }
}
