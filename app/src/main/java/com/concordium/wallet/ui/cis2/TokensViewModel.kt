package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountContract
import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.cis2.retrofit.IncorrectChecksumException
import com.concordium.wallet.ui.cis2.retrofit.MetadataApiInstance
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger
import com.walletconnect.util.Empty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.Serializable
import java.math.BigInteger

data class TokenData(
    var account: Account? = null,
    var selectedToken: Token? = null,
    var contractIndex: String = "",
    var subIndex: String = "0",
) : Serializable

class TokensViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TOKENS_NOT_LOADED = -1
        const val TOKENS_OK = 0
        const val TOKENS_EMPTY = 1
    }

    private var allowToLoadMore = true

    var tokenData = TokenData()
    var tokens: MutableList<Token> = mutableListOf()

    // Save found exact tokens to keep their selection once the search is dismissed.
    // For example, the user can look for multiple exact tokens and toggle them one by one.
    private val everFoundExactTokens: MutableList<Token> = mutableListOf()
    var exactToken: Token? = null

    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val chooseTokenInfo: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val contractAddressLoading: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    private val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val lookForTokens: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val lookForExactToken: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val updateWithSelectedTokensDone: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val stepPageBy: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tokenDetails: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val hasExistingAccountContract: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val nonSelected: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val tokenBalances: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    private val proxyRepository = ProxyRepository()
    private val contractTokensRepository: ContractTokensRepository by lazy {
        ContractTokensRepository(
            WalletDatabase.getDatabase(getApplication()).contractTokenDao()
        )
    }
    private val accountContractRepository: AccountContractRepository by lazy {
        AccountContractRepository(
            WalletDatabase.getDatabase(getApplication()).accountContractDao()
        )
    }

    /**
     * @param isFungible if set, allows loading only fungible or non-fungible
     */
    fun loadTokens(
        accountAddress: String,
        isFungible: Boolean? = null,
    ) {
        waiting.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val accountContracts = accountContractRepository.find(accountAddress)
            val contractTokens = mutableListOf<ContractToken>()
            accountContracts.forEach { accountContract ->
                contractTokens.addAll(
                    if (isFungible != null)
                        contractTokensRepository.getTokens(
                            accountAddress,
                            accountContract.contractIndex,
                            isFungible
                        )
                    else
                        contractTokensRepository.getTokens(
                            accountAddress,
                            accountContract.contractIndex,
                        )
                )

            }
            tokens.clear()
            if (isFungible == true) {
                // On fungible tab we add CCD as default at the top
                tokens.add(getCCDDefaultToken(accountAddress))
            }
            tokens.addAll(contractTokens.map {
                Token(
                    it.tokenId,
                    it.tokenId,
                    "",
                    it.tokenMetadata,
                    true,
                    it.contractIndex,
                    tokenData.subIndex,
                    false,
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    it.contractName,
                    it.tokenMetadata?.symbol ?: ""
                )
            })
            waiting.postValue(false)
        }
    }

    fun lookForTokens(
        accountAddress: String,
        from: String? = null,
    ) = viewModelScope.launch(Dispatchers.IO) {
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
            existingContractTokens.mapTo(mutableSetOf(), ContractToken::tokenId) +
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
            ).tokens.onEach {
                it.contractIndex = tokenData.contractIndex
                it.subIndex = tokenData.subIndex
            }

            isLastTokenPage = pageTokens.size < limit
            tokenPageCursor = pageTokens.lastOrNull()?.id

            loadTokensMetadata(pageTokens)
            val tokensWithMetadata = pageTokens.filter { it.tokenMetadata != null }
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
            .filterNot(Token::isCCDToken)
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
                                        correspondingToken.tokenMetadata = verifiedMetadata
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
            .filterNot { it.totalSupply == "0" }
            .filterNot(Token::isCCDToken)
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
                            correspondingToken.totalBalance = balanceItem.balance.toBigInteger()
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
                existingContractTokens.mapTo(mutableSetOf(), ContractToken::tokenId) +
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

                if (apparentToken.tokenMetadata != null) {
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
            .forEach { it.isSelected = isSelectedNow }
    }

    fun hasExistingTokens() {
        tokenData.account?.let { account ->
            viewModelScope.launch {
                val existingAccountContract =
                    accountContractRepository.find(account.address, tokenData.contractIndex)
                hasExistingAccountContract.postValue(existingAccountContract != null)
            }
        } ?: run {
            hasExistingAccountContract.postValue(false)
        }
    }

    fun updateWithSelectedTokens() {
        updateTokens(tokens + everFoundExactTokens)
    }

    private fun updateTokens(loadedTokens: Iterable<Token>) {
        tokenData.account?.let { account ->
            viewModelScope.launch(Dispatchers.IO) {
                var anyChanges = false

                val selectedTokens = loadedTokens.filter(Token::isSelected)

                // Ensure we have an AccountContract
                // when there are potential tokens to add.
                if (selectedTokens.isNotEmpty()) {
                    val accountContract =
                        accountContractRepository.find(account.address, tokenData.contractIndex)
                    if (accountContract == null) {
                        accountContractRepository.insert(
                            AccountContract(
                                id = 0,
                                accountAddress = account.address,
                                contractIndex = tokenData.contractIndex,
                            )
                        )

                        anyChanges = true
                    }
                }

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
                                tokenId = selectedToken.token,
                                contractIndex = selectedToken.contractIndex,
                                contractName = selectedToken.contractName,
                                accountAddress = account.address,
                                isFungible = !(selectedToken.tokenMetadata?.unique ?: false),
                                tokenMetadata = selectedToken.tokenMetadata,
                            )
                        )

                        anyChanges = true
                    }
                }

                // As the loaded tokens list may be partial,
                // we must only delete the unselected ones among loaded.
                // Therefore, if there is an existing token but the user haven't scrolled to it
                // in the search results, it won't be deleted.
                val loadedNotSelectedTokenIds = loadedTokens
                    .filterNot(Token::isSelected)
                    .mapTo(mutableSetOf(), Token::token)

                // Delete each loaded not selected token.
                loadedNotSelectedTokenIds.forEach { loadedNotSelectedTokenId ->
                    contractTokensRepository.find(
                        account.address,
                        tokenData.contractIndex,
                        loadedNotSelectedTokenId
                    )?.also {
                        contractTokensRepository.delete(it)
                        anyChanges = true
                    }
                }

                // If there were tokens but all of them got deleted,
                // delete the AccountContract as well.
                val existingContractTokens =
                    contractTokensRepository.getTokens(account.address, tokenData.contractIndex)
                if (existingContractTokens.isNotEmpty()
                    && loadedNotSelectedTokenIds.size == existingContractTokens.size
                ) {
                    accountContractRepository.find(account.address, tokenData.contractIndex)
                        ?.also {
                            accountContractRepository.delete(it)
                            anyChanges = true
                        }
                }

                updateWithSelectedTokensDone.postValue(anyChanges)
            }
        }
    }

    fun stepPage(by: Int) {
        stepPageBy.postValue(by)
    }

    private suspend fun getCCDDefaultToken(accountAddress: String): Token {
        val accountRepository =
            AccountRepository(WalletDatabase.getDatabase(getApplication()).accountDao())
        val account = accountRepository.findByAddress(accountAddress)
            ?: error("Account $accountAddress not found")
        return Token.ccd(account)
    }

    fun loadTokensBalances() {
        if (tokenData.account == null)
            return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                loadTokensBalances(
                    tokensToUpdate = tokens,
                    accountAddress = tokenData.account!!.address,
                )
                tokenBalances.postValue(true)
            } catch (e: Throwable) {
                handleBackendError(e)
            }
        }
    }

    fun deleteSingleToken(accountAddress: String, contractIndex: String, tokenId: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val existingContractTokens =
                contractTokensRepository.getTokens(accountAddress, contractIndex)

            contractTokensRepository.find(accountAddress, contractIndex, tokenId)
                ?.let { existingNotSelectedContractToken ->
                    contractTokensRepository.delete(existingNotSelectedContractToken)

                    if (existingContractTokens.size == 1) {
                        val existingAccountContract =
                            accountContractRepository.find(accountAddress, contractIndex)
                        if (existingAccountContract == null) {
                            nonSelected.postValue(true)
                        } else {
                            accountContractRepository.delete(existingAccountContract)
                        }
                    }
                }
        }

    fun onFindTokensDialogDismissed() {
        resetLookForTokens()
    }

    private fun resetLookForTokens() {
        tokenData.contractIndex = String.Empty
        stepPageBy.value = 0
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
