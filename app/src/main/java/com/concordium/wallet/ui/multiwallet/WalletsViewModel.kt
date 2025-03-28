package com.concordium.wallet.ui.multiwallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.multiwallet.AddAndActivateWalletUseCase
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.core.multiwallet.DeleteActiveWalletUseCase
import com.concordium.wallet.core.multiwallet.SwitchActiveWalletUseCase
import com.concordium.wallet.data.AppWalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class WalletsViewModel(application: Application) : AndroidViewModel(application) {

    private val walletRepository: AppWalletRepository = App.appCore.walletRepository
    private val addAndActivateWalletUseCase = AddAndActivateWalletUseCase()
    private val deleteActiveWalletUseCase = DeleteActiveWalletUseCase()
    private var isModifyingWallets = false
    private var isSwitchingWallet = false

    private val _listItemsLiveData = MutableLiveData<List<WalletListItem>>()
    val listItemsLiveData: LiveData<List<WalletListItem>> = _listItemsLiveData
    private val _isRemoveButtonVisibleLiveData = MutableLiveData<Boolean>()
    val isRemoveButtonVisibleLiveData: LiveData<Boolean> = _isRemoveButtonVisibleLiveData
    private val _removeButtonWalletTypeLiveData = MutableLiveData<AppWallet.Type>()
    val removeButtonWalletTypeLiveData: LiveData<AppWallet.Type> = _removeButtonWalletTypeLiveData

    private val mutableEventsFlow =
        MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = mutableEventsFlow

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

            _isRemoveButtonVisibleLiveData.postValue(wallets.size > 1)
            _removeButtonWalletTypeLiveData.postValue(activeWallet.type)
        }
    }

    fun onWalletItemClicked(item: WalletListItem.Wallet) = viewModelScope.launch {
        val wallet = item.source
            ?: return@launch

        if (wallet != App.appCore.session.activeWallet) {
            if (isSwitchingWallet) {
                return@launch
            }

            isSwitchingWallet = true

            SwitchActiveWalletUseCase()
                .invoke(
                    newActiveWallet = wallet,
                )

            mutableEventsFlow.tryEmit(
                Event.GoToMain()
            )
        }
    }

    fun onAddingSeedWalletConfirmed(import: Boolean) = viewModelScope.launch {
        if (isModifyingWallets) {
            return@launch
        }

        isModifyingWallets = true

        addAndActivateWalletUseCase(
            walletType = AppWallet.Type.SEED,
        )

        mutableEventsFlow.tryEmit(
            Event.GoToMain(
                startWithSeedImport = import,
            )
        )
    }

    fun onAddingFileWalletConfirmed() = viewModelScope.launch {
        if (isModifyingWallets) {
            return@launch
        }

        isModifyingWallets = true

        addAndActivateWalletUseCase(
            walletType = AppWallet.Type.FILE,
        )

        mutableEventsFlow.tryEmit(
            Event.GoToMain(
                startWithFileImport = true,
            )
        )
    }

    fun onRemovingWalletConfirmed() = viewModelScope.launch {
        if (isModifyingWallets) {
            return@launch
        }

        isModifyingWallets = true

        val newActiveWallet = walletRepository.getWallets()
            .find { it != App.appCore.session.activeWallet }
            ?: error("Removing is not possible when there is a single wallet")

        deleteActiveWalletUseCase(
            newActiveWallet = newActiveWallet,
        )

        mutableEventsFlow.tryEmit(
            Event.GoToMain()
        )
    }

    sealed interface Event {
        class GoToMain(
            val startWithFileImport: Boolean = false,
            val startWithSeedImport: Boolean = false,
        ) : Event
    }
}
