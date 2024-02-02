package com.concordium.wallet.ui.tokens.tokens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.backend.tokens.TokensRepository
import com.concordium.wallet.data.model.WalletMeta
import com.concordium.wallet.ui.tokens.provider.Token
import com.concordium.wallet.util.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TokensOverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val tokensRepo = TokensRepository()

    private val _onTokensReadyListLiveData: MutableLiveData<Event<List<Token>>> = MutableLiveData()
    fun onTokensReady(): LiveData<Event<List<Token>>> = _onTokensReadyListLiveData

    private val _onNextTokensReadyListLiveData: MutableLiveData<Event<List<Token>>> = MutableLiveData()
    fun onNextTokensReady(): LiveData<Event<List<Token>>> = _onNextTokensReadyListLiveData

    fun processAction(action: TokenViewAction) {
        when (action) {
            is TokenViewAction.GetTokens -> getTokens(action.walletMeta, action.offset)
            is TokenViewAction.GetNextTokens -> getNextTokens(action.walletMeta, action.offset)
        }
    }

    private fun getTokens(walletMeta: WalletMeta, offset: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val tokens = tokensRepo.getTokens(walletMeta.website, walletMeta.address, offset)
            if (tokens != null) {
                _onTokensReadyListLiveData.postValue(Event(tokens.tokens))
            }
        }
    }

    private fun getNextTokens(walletMeta: WalletMeta, offset: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val tokens = tokensRepo.getTokens(walletMeta.website, walletMeta.address, offset)
            if (tokens != null) {
                _onNextTokensReadyListLiveData.postValue(Event(tokens.tokens))
            }
        }
    }
}