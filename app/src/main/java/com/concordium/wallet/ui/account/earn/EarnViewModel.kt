package com.concordium.wallet.ui.account.earn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EarnViewModel(
    mainViewModel: MainViewModel,
    application: Application
) : AndroidViewModel(application) {

    private val transferRepository =
        TransferRepository(App.appCore.session.walletStorage.database.transferDao())

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mainViewModel.activeAccount.collect {
                it?.let(::checkBakingDelegationStatus)
            }
        }
    }

    private fun checkBakingDelegationStatus(account: Account) = viewModelScope.launch {
        _uiState.update { it.copy(loading = true) }
        val transferList = transferRepository.getAllByAccountId(account.id)

        for (transfer in transferList) {
            if (transfer.transactionType == TransactionType.LOCAL_DELEGATION)
                _uiState.update { it.copy(hasPendingDelegationTransactions = true) }
            if (transfer.transactionType == TransactionType.LOCAL_BAKER)
                _uiState.update { it.copy(hasPendingBakingTransactions = true) }
        }
        _uiState.update { it.copy(account = account, loading = false) }
    }

    data class UIState(
        val account: Account? = null,
        val hasPendingBakingTransactions: Boolean = false,
        val hasPendingDelegationTransactions: Boolean = false,
        val loading: Boolean = false
    )
}