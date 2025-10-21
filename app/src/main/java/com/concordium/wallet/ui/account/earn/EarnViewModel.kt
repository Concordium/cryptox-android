package com.concordium.wallet.ui.account.earn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.common.accountupdater.AccountUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EarnViewModel(
    val mainViewModel: MainViewModel,
    application: Application
) : AndroidViewModel(application) {

    private val transferRepository =
        TransferRepository(App.appCore.session.walletStorage.database.transferDao())
    private val accountUpdater = AccountUpdater(application, viewModelScope)

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private var updaterJob: Job? = null

    init {
        viewModelScope.launch {
            mainViewModel.activeAccount.collect {
                it?.let(::checkBakingDelegationStatus)
            }
        }
    }

    private fun checkBakingDelegationStatus(account: Account) = viewModelScope.launch {
        var hasPendingDelegationTransactions = false
        var hasPendingBakingTransactions = false

        val transferList = transferRepository.getAllByAccountId(account.id)

        for (transfer in transferList) {
            if (transfer.transactionType == TransactionType.LOCAL_DELEGATION)
                hasPendingDelegationTransactions = true
            if (transfer.transactionType == TransactionType.LOCAL_BAKER)
                hasPendingBakingTransactions = true
        }
        _uiState.update {
            it.copy(
                account = account,
                hasPendingDelegationTransactions = hasPendingDelegationTransactions,
                hasPendingBakingTransactions = hasPendingBakingTransactions,
                loading = false
            )
        }
        if (hasPendingDelegationTransactions || hasPendingBakingTransactions) {
            startAccountUpdater(account)
        } else {
            stopAccountUpdater()
        }
    }

    private fun stopAccountUpdater() {
        updaterJob?.cancel()
        updaterJob = null
    }

    private fun startAccountUpdater(account: Account) {
        stopAccountUpdater()
        updaterJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(5000L) // Initial delay before first update
                _uiState.update { it.copy(loading = true) }
                accountUpdater.updateForAccount(account)
            }
        }
    }

    data class UIState(
        val account: Account? = null,
        val hasPendingBakingTransactions: Boolean = false,
        val hasPendingDelegationTransactions: Boolean = false,
        val loading: Boolean = false
    )
}