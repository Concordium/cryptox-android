package com.concordium.wallet.ui.tokenmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.PLTRepository
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.NewPLTToken
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.toNewContractToken
import com.concordium.wallet.data.model.toNewPLTToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TokensManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val contractTokensRepository: ContractTokensRepository by lazy {
        ContractTokensRepository(
            App.appCore.session.walletStorage.database.contractTokenDao()
        )
    }
    private val pltRepository: PLTRepository by lazy {
        PLTRepository(
            App.appCore.session.walletStorage.database.protocolLevelTokenDao()
        )
    }

    private var loadTokensJob: Job? = null
    private var selectedToken: NewToken? = null

    private val _tokens = MutableStateFlow<List<NewToken>>(emptyList())
    val tokens = _tokens.asStateFlow()

    private val _waiting = MutableStateFlow(false)
    val waiting = _waiting.asStateFlow()

    fun loadTokens(
        accountAddress: String,
        isFungible: Boolean? = null,
    ) {
        loadTokensJob?.cancel()
        loadTokensJob = null

        loadTokensJob = viewModelScope.launch(Dispatchers.IO) {
            _waiting.emit(true)
            val ccdToken = getCCDDefaultToken(accountAddress)
            val contractTokens = contractTokensRepository.getTokens(
                accountAddress = accountAddress,
                isFungible = isFungible,
            ).map { it.toNewContractToken() }
            // TODO: remove hardcoded PLT address
            val pltTokens =
                pltRepository.getTokens("4GbHu8Ynnt1hc2PGhRAiwGzkXYBxnSCNJEB9dcnGEJPehRw3oo")
                    .filterNot { it.isHidden }
                    .map { it.toNewPLTToken() }

            val allTokens = (listOf(ccdToken) + contractTokens + pltTokens).sortedBy { it.addedAt }

            _tokens.emit(allTokens)
            _waiting.emit(false)
        }
    }

    fun selectToken(token: NewToken?) = viewModelScope.launch {
        selectedToken = token
    }

    fun deleteSelectedToken(accountAddress: String) = viewModelScope.launch {
        when (selectedToken) {
            is NewPLTToken -> {
                val token = selectedToken as NewPLTToken
                // TODO: remove hardcoded PLT address
                pltRepository.hideToken(
                    "4GbHu8Ynnt1hc2PGhRAiwGzkXYBxnSCNJEB9dcnGEJPehRw3oo",
                    token.tokenId
                )
            }
            is NewContractToken -> {
                val token = selectedToken as NewContractToken
                contractTokensRepository.delete(
                    accountAddress = accountAddress,
                    contractIndex = token.contractIndex,
                    token = token.token,
                )
            }
            else -> {}
        }
        loadTokens(accountAddress)
        selectToken(null)
    }

    fun selectedTokenSymbol(): String = when (selectedToken) {
        is NewPLTToken -> {
            val token = selectedToken as NewPLTToken
            token.tokenId
        }
        is NewContractToken -> {
            val token = selectedToken as NewContractToken
            token.metadata?.symbol ?: token.metadata?.name ?: ""
        }
        else -> ""
    }

    private fun getCCDDefaultToken(accountAddress: String) =
        CCDToken(accountAddress = accountAddress)
}