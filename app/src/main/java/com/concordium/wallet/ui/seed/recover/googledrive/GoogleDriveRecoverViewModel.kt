package com.concordium.wallet.ui.seed.recover.googledrive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.core.multiwallet.SwitchActiveWalletTypeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoogleDriveRecoverViewModel(application: Application): AndroidViewModel(application) {

    private val _saveSeedPhraseFlow = MutableStateFlow(false)
    val saveSeedPhraseFlow = _saveSeedPhraseFlow.asStateFlow()



    fun setSeedPhrase(seed: String, password: String) = viewModelScope.launch {
        val isSavedSuccessfully = App.appCore.session.walletStorage.setupPreferences
            .tryToSetEncryptedSeedPhrase(
                seed,
                password
            )

        if (isSavedSuccessfully) {
            SwitchActiveWalletTypeUseCase().invoke(
                newWalletType = AppWallet.Type.SEED,
            )
            App.appCore.setup.finishInitialSetup()
            App.appCore.session.walletStorage.setupPreferences.setHasCompletedOnboarding(true)
        }

        _saveSeedPhraseFlow.emit(isSavedSuccessfully)
    }
}