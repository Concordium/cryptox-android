package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
            tokensInteractor.loadTokens(accountAddress = account.address)
                .onSuccess { tokens ->
                    _uiState.value = uiState.value.copy(
                        tokens = tokens,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = Event(BackendErrorHandler.getExceptionStringRes(error))
                    )
                }
        }
    }

    data class TokensListData(
        val account: Account? = null,
        val tokens: List<NewToken> = emptyList(),
        val selectedToken: NewToken? = null,
        val isLoading: Boolean = false,
        val error: Event<Int>? = null
    )
}