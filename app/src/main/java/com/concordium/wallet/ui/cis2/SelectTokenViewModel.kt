package com.concordium.wallet.ui.cis2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.ui.common.BackendErrorHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SelectTokenViewModel(
    accountAddress: String,
    private val tokensInteractor: TokensInteractor,
) : ViewModel() {

    private val _tokenList: MutableStateFlow<List<Token>> = MutableStateFlow(emptyList())
    val tokenList = _tokenList.asStateFlow()

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _errorRes: MutableSharedFlow<Int?> = MutableStateFlow(null)
    val errorRes = _errorRes.asSharedFlow()

    init {
        viewModelScope.launch {
            tokensInteractor
                .loadTokens(
                    accountAddress = accountAddress,
                    loadBalances = true,
                    onlyTransferable = true,
                    addCCDToken = true,
                )
                .onSuccess(_tokenList::tryEmit)
                .onFailure { error ->
                    _errorRes.tryEmit(BackendErrorHandler.getExceptionStringRes(error))
                }

            _isLoading.tryEmit(false)
        }
    }
}
