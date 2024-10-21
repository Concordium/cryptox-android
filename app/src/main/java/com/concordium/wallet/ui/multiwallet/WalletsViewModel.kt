package com.concordium.wallet.ui.multiwallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.data.AppWalletRepository
import kotlinx.coroutines.launch

class WalletsViewModel(application: Application) : AndroidViewModel(application) {

    private val walletRepository: AppWalletRepository = App.appCore.walletRepository

    private val _listItemsLiveData = MutableLiveData<List<WalletListItem>>()
    val listItemsLiveData: LiveData<List<WalletListItem>> = _listItemsLiveData

    init {
        subscribeToWallets()
    }

    private fun subscribeToWallets() = viewModelScope.launch {
        walletRepository.getWalletsFlow().collect { wallets ->
            val activeWallet = App.appCore.session.activeWallet
            _listItemsLiveData.postValue(buildList {
                wallets.forEach { wallet ->
                    add(
                        WalletListItem.Wallet(
                            appWallet = wallet,
                            isSelected = wallet == activeWallet,
                        )
                    )
                }

                AppWallet.Type.values().forEach { walletType ->
                    if (wallets.none { it.type == walletType }) {
                        add(WalletListItem.AddButton(walletType))
                    }
                }
            })
        }
    }
}
