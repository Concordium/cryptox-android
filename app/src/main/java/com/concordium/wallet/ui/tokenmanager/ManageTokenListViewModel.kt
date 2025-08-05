package com.concordium.wallet.ui.tokenmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.PLTToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ManageTokenListViewModel(application: Application) : AndroidViewModel(application),
    KoinComponent {

    private val tokensInteractor by inject<TokensInteractor>()
    private var loadTokensJob: Job? = null

    private val _uiState = MutableStateFlow(TokensUIState())
    val uiState = _uiState.asStateFlow()

    fun loadTokens(accountAddress: String) {
        loadTokensJob?.cancel()
        loadTokensJob = null

        loadTokensJob = viewModelScope.launch(Dispatchers.IO) {
            updateUIState(waiting = true)
            tokensInteractor.loadTokens(
                accountAddress = accountAddress,
                loadBalances = false,
                addCCDToken = false
            )
                .onSuccess { tokens ->
                    updateUIState(
                        tokens = tokens.sortedByDescending { it.addedAt },
                        waiting = false
                    )
                }
                .onFailure {
                    updateUIState(waiting = false, error = it.message ?: "Unknown error")
                }
        }
    }

    fun selectToken(token: Token?) {
        updateUIState(selectedToken = token)
    }

    fun deleteSelectedToken(accountAddress: String) = viewModelScope.launch(Dispatchers.IO) {
        tokensInteractor.deleteToken(
            accountAddress,
            uiState.value.selectedToken ?: return@launch
        )
            .onSuccess {
                if (it) {
                    loadTokens(accountAddress)
                    selectToken(null)
                }
            }
            .onFailure {
                updateUIState(error = it.message ?: "Unknown error")
            }
    }

    fun selectedTokenSymbol(): String =
        when (uiState.value.selectedToken) {
            is PLTToken -> {
                val token = uiState.value.selectedToken as PLTToken
                token.tokenId
            }

            is NewContractToken -> {
                val token = uiState.value.selectedToken as NewContractToken
                token.metadata?.symbol ?: token.metadata?.name ?: ""
            }

            else -> ""
        }

    private fun updateUIState(
        tokens: List<Token> = uiState.value.tokens,
        selectedToken: Token? = uiState.value.selectedToken,
        waiting: Boolean = uiState.value.loading,
        error: String? = uiState.value.error
    ) {
        _uiState.value = TokensUIState(tokens, selectedToken, waiting, error)
    }

    data class TokensUIState(
        val tokens: List<Token> = emptyList(),
        val selectedToken: Token? = null,
        val loading: Boolean = false,
        val error: String? = null
    )
}