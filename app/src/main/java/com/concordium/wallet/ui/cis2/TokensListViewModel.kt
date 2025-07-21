package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.room.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TokensListViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    private val tokensInteractor by inject<TokensInteractor>()

    private val _uiState = MutableStateFlow(TokensListData())
    val uiState = _uiState.asStateFlow()

    private var loadTokensJob: Job? = null

    fun loadTokens(account: Account) {
        loadTokensJob?.cancel()
        loadTokensJob = null

        loadTokensJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = uiState.value.copy(account = account, isLoading = true)
            delay(1000)
            tokensInteractor.loadTokens(accountAddress = account.address)
                .onSuccess { tokens ->
                    tokens.forEach {
                        println("token with balance ${it.balance}")
                    }
                    _uiState.value = uiState.value.copy(
                        tokens = tokens,
                        isLoading = false
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false) }
        }
    }

    data class TokensListData(
        val account: Account? = null,
        val tokens: List<NewToken> = emptyList(),
        val selectedToken: NewToken? = null,
        var isLoading: Boolean = false
    )
}