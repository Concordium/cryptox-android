package com.concordium.wallet.ui.account.accountsoverview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.SimpleFraction
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.extension.collect
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigInteger

class AccountBalanceViewModel(
    private val accountDetailsViewModel: AccountDetailsViewModel,
    private val tokensInteractor: TokensInteractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountBalanceData())
    val uiState: StateFlow<AccountBalanceData> = _uiState

    private var loadAccountDataJob: Job? = null


    init {
        accountDetailsViewModel
            .accountUpdatedFlow
            .collect(viewModelScope) { updatedAccount ->
                loadAccountData(updatedAccount)
            }
    }

    fun onChangeCurrencyClicked() {
        App.appCore.setup.setShowBalanceInEur(_uiState.value.showBalanceInEur.not())
        _uiState.update {
            it.copy(showBalanceInEur = _uiState.value.showBalanceInEur.not())
        }
    }

    private fun loadAccountData(account: Account) {
        loadAccountDataJob?.cancel()
        loadAccountDataJob = null

        loadAccountDataJob = viewModelScope.launch(Dispatchers.IO) {
            val ccdToken = tokensInteractor.getCCDDefaultToken(
                accountAddress = account.address,
                withTotalBalance = true
            )

            _uiState.update {
                it.copy(
                    account = account,
                    ccdToken = ccdToken,
                    totalBalance = ccdToken.balance,
                    atDisposalBalance = account.balanceAtDisposal,
                    eurBalance = ccdToken.eurPerMicroCcd,
                    isStaking = ccdToken.isEarning,
                    showBalanceInEur = App.appCore.setup.showBalanceInEur
                )
            }
        }
    }

    data class AccountBalanceData(
        val account: Account? = null,
        val ccdToken: CCDToken? = null,
        val totalBalance: BigInteger = BigInteger.ZERO,
        val atDisposalBalance: BigInteger = BigInteger.ZERO,
        val eurBalance: SimpleFraction? = null,
        val isStaking: Boolean = false,
        val showBalanceInEur: Boolean = false
    )
}