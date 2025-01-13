package com.concordium.wallet.ui.multiwallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.core.multiwallet.SwitchActiveWalletUseCase
import com.concordium.wallet.data.AppWalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class WalletSwitchViewModel(application: Application) : AndroidViewModel(application) {

    private val walletRepository: AppWalletRepository = App.appCore.walletRepository
    private var isSwitchingWallet = false

    private val _isViewVisibleLiveData = MutableLiveData<Boolean>()
    val isViewVisibleLiveData: LiveData<Boolean> = _isViewVisibleLiveData
    private val _activeWalletTypeLiveData = MutableLiveData(App.appCore.session.activeWallet.type)
    val activeWalletTypeLiveData: LiveData<AppWallet.Type> = _activeWalletTypeLiveData
    private val mutableSwitchesFlow = MutableSharedFlow<AppWallet>(extraBufferCapacity = 10)
    val switchesFlow: Flow<AppWallet> = mutableSwitchesFlow

    init {
        subscribeToWallets()
    }

    private fun subscribeToWallets() = viewModelScope.launch {
        walletRepository.getWalletsFlow().collect { wallets ->
            _isViewVisibleLiveData.postValue(wallets.size > 1)
            _activeWalletTypeLiveData.postValue(App.appCore.session.activeWallet.type)
        }
    }

    fun onSwitchClicked() = viewModelScope.launch {
        if (isSwitchingWallet) {
            return@launch
        }

        isSwitchingWallet = true

        val newActiveWallet = walletRepository.getWallets()
            .find { it != App.appCore.session.activeWallet }
            ?: error("Switching is not possible when there is a single wallet")

        SwitchActiveWalletUseCase().invoke(newActiveWallet)
        _activeWalletTypeLiveData.postValue(newActiveWallet.type)

        mutableSwitchesFlow.tryEmit(newActiveWallet)

        isSwitchingWallet = false
    }
}
