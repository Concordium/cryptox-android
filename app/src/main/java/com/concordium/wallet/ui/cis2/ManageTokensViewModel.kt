package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.tokens.TokensInteractor
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.PLTRepository
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.toContractToken
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.ContractTokenEntity
import com.concordium.wallet.data.room.ProtocolLevelTokenEntity
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Serializable

data class TokenData(
    var account: Account? = null,
    var contractIndex: String = "",
    var subIndex: String = "0",
    var hasPendingDelegationTransactions: Boolean = false,
    var hasPendingValidationTransactions: Boolean = false,
) : Serializable

class ManageTokensViewModel(
    application: Application,
) : AndroidViewModel(application),
    KoinComponent {

    companion object {
        const val TOKENS_NOT_LOADED = -1
        const val TOKENS_OK = 0
        const val TOKENS_EMPTY = 1
        const val TOKENS_SELECTED = 2
    }

    private var allowToLoadMore = true
    var tokenData = TokenData()
    var tokens: MutableList<Token> = mutableListOf()

    // Save found exact tokens to keep their selection once the search is dismissed.
    // For example, the user can look for multiple exact tokens and toggle them one by one.
    private val everFoundExactTokens: MutableList<Token> = mutableListOf()
    private val changedTokensList: MutableList<Token> = mutableListOf()
    var exactToken: Token? = null

    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val contractAddressLoading: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    private val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val lookForTokens: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val lookForExactToken: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val updateWithSelectedTokensDone: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val tokenDetails: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val nonSelected: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val selectedTokensChanged: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }

    private val contractTokensRepository: ContractTokensRepository by lazy {
        ContractTokensRepository(
            App.appCore.session.walletStorage.database.contractTokenDao()
        )
    }
    private val pltRepository = PLTRepository(
        App.appCore.session.walletStorage.database.protocolLevelTokenDao()
    )
    private val tokensInteractor by inject<TokensInteractor>()

    private var lookForExactTokenJob: Job? = null

    fun lookForTokens(
        accountAddress: String,
        from: String? = null,
    ) = viewModelScope.launch(Dispatchers.IO) {
        changedTokensList.clear()
        selectedTokensChanged.postValue(changedTokensList.isNotEmpty())

        if (from != null && !allowToLoadMore)
            return@launch

        allowToLoadMore = false

        if (from == null) {
            tokens.clear()
            lookForTokens.postValue(TOKENS_NOT_LOADED)
            contractAddressLoading.postValue(true)
        }

        if (tokenData.contractIndex.isDigitsOnly()) {
            lookForCIS2Tokens(
                accountAddress = accountAddress,
                from = from
            )
        } else {
            lookForPLTTokens(accountAddress)
        }
    }

    private fun lookForCIS2Tokens(
        accountAddress: String,
        from: String? = null,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val existingContractTokens =
            contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
                .map { it.toContractToken() }
        val selectedTokenIds =
            existingContractTokens.mapTo(mutableSetOf(), ContractToken::token) +
                    everFoundExactTokens.asSequence()
                        .filterIsInstance<ContractToken>()
                        .filter(ContractToken::isSelected)
                        .mapTo(mutableSetOf(), ContractToken::token)

        val pageLimit = 20
        tokensInteractor.loadCIS2TokensWithBalance(
            accountAddress = accountAddress,
            contractIndex = tokenData.contractIndex,
            subIndex = tokenData.subIndex,
            limit = pageLimit,
            from = from,
        ).onSuccess { tokensPage ->
            tokens.addAll(
                tokensPage.onEach {
                    it.isSelected = it.token in selectedTokenIds
                }
            )
            contractAddressLoading.postValue(false)
            allowToLoadMore = tokensPage.size >= pageLimit

            if (tokens.isEmpty() && !allowToLoadMore) {
                lookForTokens.postValue(TOKENS_EMPTY)
            } else {
                lookForTokens.postValue(TOKENS_OK)
            }
        }.onFailure {
            handleBackendError(it)
            allowToLoadMore = true
            contractAddressLoading.postValue(false)
        }
    }

    private fun lookForPLTTokens(accountAddress: String) = viewModelScope.launch(Dispatchers.IO) {
        val existingToken =
            pltRepository
                .find(
                    accountAddress = accountAddress,
                    tokenId = tokenData.contractIndex
                )
                ?.toProtocolLevelToken()
                ?.takeUnless(ProtocolLevelToken::isHidden)

        val selectedTokenIds = everFoundExactTokens
            .filterIsInstance<ProtocolLevelToken>()
            .filter(ProtocolLevelToken::isSelected)
            .mapTo(mutableSetOf()) { it.tokenId.lowercase() }
            .apply {
                existingToken?.let { add(it.tokenId.lowercase()) }
            }

        tokensInteractor
            .loadProtocolLevelTokenWithMetadata(
                accountAddress = accountAddress,
                tokenId = tokenData.contractIndex,
            )
            .onSuccess { loadedToken ->
                tokens.add(
                    loadedToken.copy(
                        isSelected = loadedToken.tokenId.lowercase() in selectedTokenIds,
                    )
                )
                contractAddressLoading.postValue(false)
                lookForTokens.postValue(TOKENS_OK)
            }
            .onFailure {
                Log.e("Failed to load PLT token by ID: ${tokenData.contractIndex}", it)
                handleBackendError(it)
                contractAddressLoading.postValue(false)
                lookForTokens.postValue(TOKENS_EMPTY)
            }
    }

    fun lookForExactToken(
        accountAddress: String,
        apparentTokenId: String,
    ) {
        lookForExactTokenJob?.cancel()
        lookForExactTokenJob = viewModelScope.launch(Dispatchers.IO) {
            val existingContractTokens =
                contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
                    .map { it.toContractToken() }
            val selectedTokenIds =
                existingContractTokens.mapTo(mutableSetOf(), ContractToken::token) +
                        (tokens.asSequence() + everFoundExactTokens.asSequence())
                            .filterIsInstance<ContractToken>()
                            .filter(ContractToken::isSelected)
                            .mapTo(mutableSetOf(), ContractToken::token)

            val apparentToken = ContractToken(
                token = apparentTokenId,
                contractIndex = tokenData.contractIndex,
                subIndex = tokenData.subIndex,
                isSelected = apparentTokenId in selectedTokenIds,
            )

            try {
                awaitAll(
                    async {
                        tokensInteractor.loadCIS2TokensMetadata(listOf(apparentToken))
                    },
                    async {
                        tokensInteractor.loadTokensBalances(
                            accountAddress = accountAddress,
                            tokens = listOf(apparentToken),
                        )
                    },
                )

                if (apparentToken.metadata != null || apparentToken.metadataError.isNotEmpty()) {
                    everFoundExactTokens.add(apparentToken)
                    exactToken = apparentToken
                    lookForExactToken.postValue(TOKENS_OK)
                } else {
                    exactToken = null
                    lookForExactToken.postValue(TOKENS_EMPTY)
                }
            } catch (e: Throwable) {
                handleBackendError(e)
                exactToken = null
                lookForExactToken.postValue(TOKENS_EMPTY)
            }
        }
    }

    fun dismissExactTokenLookup() {
        lookForExactTokenJob?.cancel()
        exactToken = null
        lookForExactToken.value = TOKENS_NOT_LOADED
    }

    fun toggleNewToken(token: Token) {
        val isSelectedNow = !token.isSelected
        (tokens.asSequence() + everFoundExactTokens.asSequence())
            .filter {
                if (token is ContractToken)
                    (it as ContractToken).token == token.token
                else
                    it.symbol == token.symbol
            }
            .forEach {
                it.isSelected = isSelectedNow

                if (changedTokensList.contains(it)) {
                    changedTokensList.remove(it)
                } else {
                    changedTokensList.add(it)
                }
                selectedTokensChanged.postValue(changedTokensList.isNotEmpty())
            }
    }

    fun updateWithSelectedTokens() {
        updateTokens(tokens + everFoundExactTokens)
    }

    private fun updateTokens(loadedTokens: List<Token>) {
        tokenData.account?.let { account ->
            viewModelScope.launch(Dispatchers.IO) {
                updateCIS2Tokens(
                    accountAddress = account.address,
                    loadedTokens = loadedTokens
                )
                updatePLTTokens(
                    accountAddress = account.address,
                    loadedTokens = loadedTokens
                )
                updateWithSelectedTokensDone.postValue(changedTokensList.isNotEmpty())
            }
        }
    }

    private suspend fun updatePLTTokens(accountAddress: String, loadedTokens: List<Token>) {
        loadedTokens
            .filter(Token::isSelected)
            .filterIsInstance<ProtocolLevelToken>()
            .forEach { selectedToken ->
                val existingPLToken = pltRepository.find(accountAddress, selectedToken.tokenId)
                if (existingPLToken != null) {
                    pltRepository.updateToken(
                        updatedEntity = existingPLToken.copy(
                            isHidden = false,
                            metadata = selectedToken.metadata,
                        )
                    )
                } else {
                    pltRepository.insert(
                        ProtocolLevelTokenEntity(selectedToken)
                    )
                }
            }
        // Hide each loaded not selected token.
        loadedTokens
            .filterNot(Token::isSelected)
            .filterIsInstance<ProtocolLevelToken>()
            .mapTo(mutableSetOf(), ProtocolLevelToken::tokenId)
            .forEach { loadedNotSelectedTokenId ->
                pltRepository.hideToken(
                    accountAddress = accountAddress,
                    tokenId = loadedNotSelectedTokenId,
                )
            }
    }

    private suspend fun updateCIS2Tokens(
        accountAddress: String,
        loadedTokens: List<Token>,
    ) {
        loadedTokens
            .filter(Token::isSelected)
            .filterIsInstance<ContractToken>()
            .forEach { selectedToken ->
                val existingContractToken =
                    contractTokensRepository.find(
                        accountAddress,
                        selectedToken.contractIndex,
                        selectedToken.token
                    )
                if (existingContractToken == null) {
                    contractTokensRepository.insert(
                        ContractTokenEntity(
                            id = 0,
                            token = selectedToken.token,
                            contractIndex = selectedToken.contractIndex,
                            contractName = selectedToken.contractName,
                            accountAddress = accountAddress,
                            metadata = selectedToken.metadata,
                            isNewlyReceived = false,
                            addedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

        // Delete each loaded not selected token.
        loadedTokens
            .filterNot(Token::isSelected)
            .filterIsInstance<ContractToken>()
            .mapTo(mutableSetOf(), ContractToken::token)
            .forEach { loadedNotSelectedToken ->
                contractTokensRepository.delete(
                    accountAddress = accountAddress,
                    contractIndex = tokenData.contractIndex,
                    token = loadedNotSelectedToken,
                )
            }
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }

    // Returns the last token ID in the list of tokens for endless loading
    fun lastTokenId() = tokens.filterIsInstance<ContractToken>().last().uid
}
