package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.backend.price.TokenPriceRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.toToken
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.ui.cis2.retrofit.IncorrectChecksumException
import com.concordium.wallet.ui.cis2.retrofit.MetadataApiInstance
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger
import com.reown.util.Empty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Serializable

data class TokenData(
    var account: Account? = null,
    var selectedToken: Token? = null,
    var contractIndex: String = "",
    var subIndex: String = "0",
    var hasPendingDelegationTransactions: Boolean = false,
    var hasPendingValidationTransactions: Boolean = false,
) : Serializable

class TokensViewModel(
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

    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val chooseTokenInfo: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val contractAddressLoading: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    private val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val lookForTokens: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val lookForExactToken: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val updateWithSelectedTokensDone: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val tokenDetails: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val hasExistingTokens: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val nonSelected: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val tokenBalances: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val selectedTokensChanged: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }

    private var loadTokensJob: Job? = null
    private val proxyRepository = ProxyRepository()
    private val contractTokensRepository: ContractTokensRepository by lazy {
        ContractTokensRepository(
            App.appCore.session.walletStorage.database.contractTokenDao()
        )
    }
    private val tokenPriceRepository by inject<TokenPriceRepository>()

    /**
     * @param isFungible if set, allows loading only fungible or non-fungible
     */
    fun loadTokens(
        accountAddress: String,
        isFungible: Boolean? = null,
    ) {
        loadTokensJob?.cancel()
        loadTokensJob = null

        loadTokensJob = viewModelScope.launch(Dispatchers.IO) {
            waiting.postValue(true)
            val ccdToken = getCCDDefaultToken(accountAddress)
            val contractTokens = contractTokensRepository.getTokens(
                accountAddress = accountAddress,
                isFungible = isFungible,
            )
            withContext(Dispatchers.Main) {
                tokens.clear()
                // Add CCD as default at the top
                tokens.add(ccdToken)
                tokens.addAll(contractTokens.map {
                    Token(
                        contractToken = it,
                        isSelected = true,
                    )
                })
            }
            loadTokensBalances()
            waiting.postValue(false)
        }
    }

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
            everFoundExactTokens.clear()
            lookForTokens.postValue(TOKENS_NOT_LOADED)
            contractAddressLoading.postValue(true)
        }

        val existingContractTokens =
            contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
        val selectedTokenIds =
            existingContractTokens.mapTo(mutableSetOf(), ContractToken::token) +
                    everFoundExactTokens.asSequence()
                        .filter(Token::isSelected)
                        .mapTo(mutableSetOf(), Token::token)

        try {
            val pageLimit = 20
            val pageTokens = getFullyLoadedTokensPage(
                accountAddress = accountAddress,
                limit = pageLimit,
                from = from,
            ).onEach {
                it.isSelected = it.token in selectedTokenIds
            }

            tokens.addAll(pageTokens)
            contractAddressLoading.postValue(false)
            allowToLoadMore = pageTokens.size >= pageLimit

            if (tokens.isEmpty() && !allowToLoadMore) {
                lookForTokens.postValue(TOKENS_EMPTY)
            } else {
                lookForTokens.postValue(TOKENS_OK)
            }
        } catch (e: Throwable) {
            handleBackendError(e)
            allowToLoadMore = true
            contractAddressLoading.postValue(false)
        }
    }

    /**
     * @return page of tokens with fully loaded metadata and balances.
     * If its size is smaller than [limit], consider this page the last one.
     */
    private suspend fun getFullyLoadedTokensPage(
        accountAddress: String,
        limit: Int,
        from: String? = null,
    ): List<Token> {
        val fullyLoadedTokens = mutableListOf<Token>()

        var tokenPageCursor = from
        var isLastTokenPage = false
        while (fullyLoadedTokens.size < limit && !isLastTokenPage) {
            val pageTokens = proxyRepository.getCIS2Tokens(
                index = tokenData.contractIndex,
                subIndex = tokenData.subIndex,
                limit = limit,
                from = tokenPageCursor,
            ).tokens
                .map { it.toToken() }
                .onEach {
                    it.contractIndex = tokenData.contractIndex
                    it.subIndex = tokenData.subIndex
                }

            pageTokens.forEach {
                println("getFullyLoadedTokensPage, token: $it")
            }

            isLastTokenPage = pageTokens.size < limit
            tokenPageCursor = pageTokens.lastOrNull()?.uid

            loadTokensMetadata(pageTokens)
            val tokensWithMetadata = pageTokens.filter { it.metadata != null }
            loadTokensBalances(
                tokensToUpdate = tokensWithMetadata,
                accountAddress = accountAddress,
            )

            fullyLoadedTokens.addAll(tokensWithMetadata)
        }

        return fullyLoadedTokens
    }

    private suspend fun loadTokensMetadata(tokensToUpdate: List<Token>) {
        val tokensByContract: Map<String, List<Token>> = tokensToUpdate
            .filterNot(Token::isCcd)
            .groupBy(Token::contractIndex)

        tokensByContract.forEach { (contractIndex, contractTokens) ->
            val contractSubIndex = contractTokens.firstOrNull()?.subIndex
                ?: return@forEach

            contractTokens
                .chunked(ProxyRepository.CIS_2_TOKEN_METADATA_MAX_TOKEN_IDS)
                .forEach { contractTokensChunk ->
                    val commaSeparatedChunkTokenIds = contractTokensChunk.joinToString(
                        separator = ",",
                        transform = Token::token,
                    )

                    try {
                        val ciS2TokensMetadata = proxyRepository.getCIS2TokenMetadataV1(
                            index = contractIndex,
                            subIndex = contractSubIndex,
                            tokenIds = commaSeparatedChunkTokenIds,
                        )

                        ciS2TokensMetadata.metadata
                            .filterNot { it.metadataURL.isBlank() }
                            // Request the actual metadata in parallel.
                            .map { metadataItem ->
                                viewModelScope.async(Dispatchers.IO) {
                                    try {
                                        val verifiedMetadata = MetadataApiInstance.safeMetadataCall(
                                            url = metadataItem.metadataURL,
                                            checksum = metadataItem.metadataChecksum,
                                        ).getOrThrow()
                                        val correspondingToken = contractTokens.first {
                                            it.token == metadataItem.tokenId
                                        }
                                        correspondingToken.metadata = verifiedMetadata
                                        correspondingToken.contractName =
                                            ciS2TokensMetadata.contractName
                                    } catch (e: IncorrectChecksumException) {
                                        Log.w(
                                            "Metadata checksum incorrect:\n" +
                                                    "metadataItem=$metadataItem"
                                        )
                                    } catch (e: Throwable) {
                                        Log.e(
                                            "Failed to load metadata:\n" +
                                                    "metadataItem=$metadataItem" +
                                                    e
                                        )
                                    }
                                }
                            }
                            .awaitAll()
                    } catch (e: Throwable) {
                        Log.e(
                            "Failed to load metadata chunk:\n" +
                                    "contract=$contractIndex:$contractSubIndex,\n" +
                                    "chunkTokenIds=$commaSeparatedChunkTokenIds",
                            e
                        )
                    }
                }
        }
    }

    private suspend fun loadTokensBalances(
        tokensToUpdate: List<Token>,
        accountAddress: String,
    ) {
        val tokensByContract: Map<String, List<Token>> = tokensToUpdate
            .filterNot(Token::isCcd)
            .groupBy(Token::contractIndex)

        tokensByContract.forEach { (contractIndex, contractTokens) ->
            val contractSubIndex = contractTokens.firstOrNull()?.subIndex
                ?: return@forEach

            contractTokens
                .chunked(ProxyRepository.CIS_2_TOKEN_BALANCE_MAX_TOKEN_IDS)
                .forEach { contractTokensChunk ->
                    val commaSeparatedChunkTokenIds = contractTokensChunk.joinToString(
                        separator = ",",
                        transform = Token::token,
                    )

                    try {
                        proxyRepository.getCIS2TokenBalanceV1(
                            index = contractIndex,
                            subIndex = contractSubIndex,
                            accountAddress = accountAddress,
                            tokenIds = commaSeparatedChunkTokenIds,
                        ).forEach { balanceItem ->
                            val correspondingToken = contractTokens.first {
                                it.token == balanceItem.tokenId
                            }
                            correspondingToken.balance = balanceItem.balance.toBigInteger()
                        }
                    } catch (e: Throwable) {
                        Log.e(
                            "Failed to load balances chunk:\n" +
                                    "contract=$contractIndex:$contractSubIndex,\n" +
                                    "accountAddress=$accountAddress,\n" +
                                    "chunkTokenIds=$commaSeparatedChunkTokenIds",
                            e
                        )
                    }
                }
        }
    }

    private var lookForExactTokenJob: Job? = null
    fun lookForExactToken(
        accountAddress: String,
        apparentTokenId: String,
    ) {
        lookForExactTokenJob?.cancel()
        lookForExactTokenJob = viewModelScope.launch(Dispatchers.IO) {
            val existingContractTokens =
                contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
            val selectedTokenIds =
                existingContractTokens.mapTo(mutableSetOf(), ContractToken::token) +
                        (tokens.asSequence() + everFoundExactTokens.asSequence())
                            .filter(Token::isSelected)
                            .mapTo(mutableSetOf(), Token::token)

            val apparentToken = Token(
                token = apparentTokenId,
                contractIndex = tokenData.contractIndex,
                subIndex = tokenData.subIndex,
                isSelected = apparentTokenId in selectedTokenIds,
            )

            try {
                awaitAll(
                    async {
                        loadTokensMetadata(listOf(apparentToken))
                    },
                    async {
                        loadTokensBalances(
                            tokensToUpdate = listOf(apparentToken),
                            accountAddress = accountAddress,
                        )
                    },
                )

                if (apparentToken.metadata != null) {
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
            .filter { it.token == token.token }
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

    fun checkExistingTokens() = viewModelScope.launch(Dispatchers.IO) {
        tokenData.account.also { account ->
            if (account != null) {
                hasExistingTokens.postValue(
                    contractTokensRepository.getTokens(
                        accountAddress = account.address,
                    ).isNotEmpty()
                )
            } else {
                hasExistingTokens.postValue(false)
            }
        }
    }

    fun updateWithSelectedTokens() {
        updateTokens(tokens + everFoundExactTokens)
    }

    private fun updateTokens(loadedTokens: Iterable<Token>) {
        tokenData.account?.let { account ->
            viewModelScope.launch(Dispatchers.IO) {
                val selectedTokens = loadedTokens.filter(Token::isSelected)

                // Add each selected token if missing.
                selectedTokens.forEach { selectedToken ->
                    val existingContractToken =
                        contractTokensRepository.find(
                            account.address,
                            selectedToken.contractIndex,
                            selectedToken.token
                        )
                    if (existingContractToken == null) {
                        contractTokensRepository.insert(
                            ContractToken(
                                id = 0,
                                token = selectedToken.token,
                                contractIndex = selectedToken.contractIndex,
                                contractName = selectedToken.contractName,
                                accountAddress = account.address,
                                isFungible = !selectedToken.isUnique,
                                tokenMetadata = selectedToken.metadata,
                                isNewlyReceived = false,
                            )
                        )
                    }
                }
                // As the loaded tokens list may be partial,
                // we must only delete the unselected ones among loaded.
                // Therefore, if there is an existing token but the user haven't scrolled to it
                // in the search results, it won't be deleted.
                val loadedNotSelectedTokens = loadedTokens
                    .filterNot(Token::isSelected)
                    .mapTo(mutableSetOf(), Token::token)

                // Delete each loaded not selected token.
                loadedNotSelectedTokens.forEach { loadedNotSelectedToken ->
                    contractTokensRepository.delete(
                        accountAddress = account.address,
                        contractIndex = tokenData.contractIndex,
                        token = loadedNotSelectedToken,
                    )
                }
                updateWithSelectedTokensDone.postValue(changedTokensList.isNotEmpty())
            }
        }
    }

    private suspend fun getCCDDefaultToken(accountAddress: String): Token {
        val accountRepository =
            AccountRepository(App.appCore.session.walletStorage.database.accountDao())
        val account = accountRepository.findByAddress(accountAddress)
            ?: error("Account $accountAddress not found")
        val eurPerMicroCcd = tokenPriceRepository.getEurPerMicroCcd()
            .getOrNull()

        return Token.ccd(
            account,
            eurPerMicroCcd = eurPerMicroCcd,
        )
    }

    fun loadTokensBalances() {
        if (tokenData.account == null)
            return

        viewModelScope.launch(Dispatchers.IO) {
            tokenBalances.postValue(false)
            try {
                val ccdToken = getCCDDefaultToken(tokenData.account!!.address)
                tokens.find { it.isCcd }?.apply {
                    balance = ccdToken.balance
                    isEarning = ccdToken.isEarning
                }

                loadTokensBalances(
                    tokensToUpdate = tokens,
                    accountAddress = tokenData.account!!.address,
                )
                tokenBalances.postValue(true)
            } catch (e: Throwable) {
                handleBackendError(e)
                tokenBalances.postValue(true)
            }
        }
    }

    fun deleteSelectedToken() = viewModelScope.launch {
        contractTokensRepository.delete(
            accountAddress = tokenData.account!!.address,
            contractIndex = tokenData.selectedToken!!.contractIndex,
            token = tokenData.selectedToken!!.token,
        )
    }

    fun unmarkNewlyReceivedSelectedToken() = viewModelScope.launch {
        contractTokensRepository.unmarkNewlyReceived(
            tokenUid = tokenData.selectedToken!!.uid,
        )
    }

    fun onFindTokensDialogDismissed() {
        resetLookForTokens()
    }

    private fun resetLookForTokens() {
        tokenData.contractIndex = String.Empty
        lookForTokens.value = TOKENS_NOT_LOADED
        lookForExactToken.value = TOKENS_NOT_LOADED
        everFoundExactTokens.clear()
        exactToken = null
        allowToLoadMore = true
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
