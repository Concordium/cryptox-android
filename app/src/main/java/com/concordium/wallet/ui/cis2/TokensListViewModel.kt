package com.concordium.wallet.ui.cis2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.extension.collect
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TokensListViewModel(
    private val accountDetailsViewModel: AccountDetailsViewModel,
    private val tokensInteractor: TokensInteractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TokensListData())
    val uiState = _uiState.asStateFlow()

    private var loadTokensJob: Job? = null

    init {
        accountDetailsViewModel
            .activeAccount
            .map { it?.id }
            .distinctUntilChanged()
            .collect(viewModelScope) {
                reset()
            }

        accountDetailsViewModel
            .accountUpdatedFlow
            .collect(viewModelScope) { updatedAccount ->
                loadTokens(updatedAccount)
            }
    }

    fun resetSelectedToken() = viewModelScope.launch {
        _uiState.update { it.copy(selectedToken = null) }
    }

    private fun reset() {
        _uiState.tryEmit(TokensListData())
    }

    private fun loadTokens(
        account: Account,
    ) {
        loadTokensJob?.cancel()
        loadTokensJob = null

        loadTokensJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            tokensInteractor.loadTokens(
                accountAddress = account.address,
                addCCDToken = false
            )
                .onSuccess { tokens ->
                    _uiState.update {
                        it.copy(
                            tokens = tokens,
                            isLoading = false
                        )
                    }
                    goToTokenDetails(tokens)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = Event(BackendErrorHandler.getExceptionStringRes(error))
                        )
                    }
                }
        }
    }

    private suspend fun goToTokenDetails(tokens: List<Token>) {
        val notificationToken = accountDetailsViewModel.notificationToken.first()

        notificationToken?.let {
            val token = when (notificationToken) {
                is ContractToken -> {
                    tokens
                        .filterIsInstance<ContractToken>()
                        .find { it.uid == notificationToken.uid }
                }

                is ProtocolLevelToken -> {
                    tokens
                        .filterIsInstance<ProtocolLevelToken>()
                        .find { it.tokenId == notificationToken.tokenId }
                }

                else -> null
            }

            token?.let {
                _uiState.update {
                    it.copy(selectedToken = token)
                }
            }
        }
    }

    data class TokensListData(
        val tokens: List<Token> = emptyList(),
        val selectedToken: Token? = null,
        val isLoading: Boolean = true,
        val error: Event<Int>? = null,
    )
}
