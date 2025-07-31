package com.concordium.wallet.ui.cis2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.model.NewToken
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TokensListViewModel(
    accountDetailsViewModel: AccountDetailsViewModel,
    private val tokensInteractor: TokensInteractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TokensListData())
    val uiState = _uiState.asStateFlow()

    private var loadTokensJob: Job? = null

    init {
        accountDetailsViewModel
            .activeAccount
            .map { it.id }
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

    private fun reset() {
        _uiState.tryEmit(TokensListData())
    }

    private fun loadTokens(
        account: Account,
        onlyTransferable: Boolean = false,
    ) {
        loadTokensJob?.cancel()
        loadTokensJob = null

        loadTokensJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = uiState.value.copy(
                isLoading = true,
            )
            tokensInteractor.loadTokens(
                accountAddress = account.address,
                onlyTransferable = onlyTransferable,
            )
                .onSuccess { tokens ->
                    _uiState.value = uiState.value.copy(
                        tokens = tokens.sortedBy { it.addedAt },
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
        val tokens: List<NewToken> = emptyList(),
        val selectedToken: NewToken? = null,
        val isLoading: Boolean = true,
        val error: Event<Int>? = null,
    )
}
