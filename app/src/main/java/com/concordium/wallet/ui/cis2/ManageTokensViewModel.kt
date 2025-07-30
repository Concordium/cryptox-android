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
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.toNewContractToken
import com.concordium.wallet.data.model.toPLTToken
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.data.room.ProtocolLevelToken
import com.concordium.wallet.ui.cis2.retrofit.IncorrectChecksumException
import com.concordium.wallet.ui.cis2.retrofit.MetadataApiInstance
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
    var selectedToken: Token? = null,
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
    var tokens: MutableList<NewToken> = mutableListOf()

    // Save found exact tokens to keep their selection once the search is dismissed.
    // For example, the user can look for multiple exact tokens and toggle them one by one.
    private val everFoundExactTokens: MutableList<NewToken> = mutableListOf()
    private val changedTokensList: MutableList<NewToken> = mutableListOf()
    var exactToken: NewToken? = null

    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val contractAddressLoading: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    private val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val lookForTokens: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val lookForExactToken: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val updateWithSelectedTokensDone: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val tokenDetails: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val nonSelected: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val selectedTokensChanged: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }

    private val proxyRepository = ProxyRepository()
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
        from: String? = null
    ) = viewModelScope.launch(Dispatchers.IO) {
        val existingContractTokens =
            contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
                .map { it.toNewContractToken() }
        val selectedTokenIds =
            existingContractTokens.mapTo(mutableSetOf(), NewContractToken::token) +
                    everFoundExactTokens.asSequence()
                        .filterIsInstance<NewContractToken>()
                        .filter(NewContractToken::isSelected)
                        .mapTo(mutableSetOf(), NewContractToken::token)

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

    private fun lookForPLTTokens(accountAddress: String) = viewModelScope.launch(Dispatchers.IO) {
        val existingPLTToken = pltRepository.find(
            accountAddress = accountAddress,
            tokenId = tokenData.contractIndex
        )?.toPLTToken()

        val existingPLTTokenIds = everFoundExactTokens
            .filterIsInstance<PLTToken>()
            .mapTo(mutableSetOf()) { it.tokenId.lowercase() }
            .apply {
                existingPLTToken?.let { add(it.tokenId.lowercase()) }
            }

        existingPLTToken?.let {
            if (it.isHidden) {
                tokens.add(it.copy(isSelected = false))
            } else {
                tokens.add(it)
            }
            contractAddressLoading.postValue(false)
            lookForTokens.postValue(TOKENS_OK)
        } ?: run {
            tokensInteractor.getPLTTokenById(tokenData.contractIndex)
                .onSuccess {
                    tokens.add(
                        it.toPLTToken(
                            accountAddress = accountAddress,
                            isSelected = it.tokenId.lowercase() in existingPLTTokenIds
                        )
                    )
                    contractAddressLoading.postValue(false)
                    if (tokens.isEmpty()) {
                        lookForTokens.postValue(TOKENS_EMPTY)
                    } else {
                        lookForTokens.postValue(TOKENS_OK)
                    }
                }
                .onFailure {
                    Log.e("Failed to load PLT token by ID: ${tokenData.contractIndex}", it)
                    handleBackendError(it)
                    contractAddressLoading.postValue(false)
                    lookForTokens.postValue(TOKENS_EMPTY)
                }
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
    ): List<NewContractToken> {
        val fullyLoadedTokens = mutableListOf<NewToken>()

        var tokenPageCursor = from
        var isLastTokenPage = false
        while (fullyLoadedTokens.size < limit && !isLastTokenPage) {
            val pageTokens = proxyRepository.getCIS2Tokens(
                index = tokenData.contractIndex,
                subIndex = tokenData.subIndex,
                limit = limit,
                from = tokenPageCursor,
            ).tokens
                .map { it.toNewContractToken() }
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
            tokensInteractor.loadTokensBalances(
                accountAddress = accountAddress,
                tokens = tokensWithMetadata
            )

            fullyLoadedTokens.addAll(tokensWithMetadata)
        }

        return fullyLoadedTokens.map { it as NewContractToken }
    }

    private suspend fun loadTokensMetadata(tokensToUpdate: List<NewContractToken>) {
        val tokensByContract: Map<String, List<NewContractToken>> = tokensToUpdate
            .groupBy(NewContractToken::contractIndex)

        tokensByContract.forEach { (contractIndex, contractTokens) ->
            val contractSubIndex = contractTokens.firstOrNull()?.subIndex
                ?: return@forEach

            contractTokens
                .chunked(ProxyRepository.CIS_2_TOKEN_METADATA_MAX_TOKEN_IDS)
                .forEach { contractTokensChunk ->
                    val commaSeparatedChunkTokenIds = contractTokensChunk.joinToString(
                        separator = ",",
                        transform = NewContractToken::token,
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

    fun lookForExactToken(
        accountAddress: String,
        apparentTokenId: String,
    ) {
        lookForExactTokenJob?.cancel()
        lookForExactTokenJob = viewModelScope.launch(Dispatchers.IO) {
            val existingContractTokens =
                contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
                    .map { it.toNewContractToken() }
            val selectedTokenIds =
                existingContractTokens.mapTo(mutableSetOf(), NewContractToken::token) +
                        (tokens.asSequence() + everFoundExactTokens.asSequence())
                            .filterIsInstance<NewContractToken>()
                            .filter(NewContractToken::isSelected)
                            .mapTo(mutableSetOf(), NewContractToken::token)

            val apparentToken = NewContractToken(
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
                        tokensInteractor.loadTokensBalances(
                            accountAddress = accountAddress,
                            tokens = listOf(apparentToken),
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

    fun toggleNewToken(token: NewToken) {
        val isSelectedNow = !token.isSelected
        (tokens.asSequence() + everFoundExactTokens.asSequence())
            .filter {
                if (token is NewContractToken)
                    (it as NewContractToken).token == token.token
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

    private fun updateTokens(loadedTokens: List<NewToken>) {
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

    private suspend fun updatePLTTokens(accountAddress: String, loadedTokens: List<NewToken>) {
        loadedTokens
            .filter(NewToken::isSelected)
            .filterIsInstance<PLTToken>()
            .forEach { selectedToken ->
                val existingPLToken = pltRepository.find(accountAddress, selectedToken.tokenId)
                existingPLToken?.let {
                    if (it.isHidden) {
                        pltRepository.unhideToken(
                            accountAddress = accountAddress,
                            tokenId = selectedToken.tokenId
                        )
                    }
                } ?: run {
                    pltRepository.insert(
                        ProtocolLevelToken(
                            tokenId = selectedToken.tokenId,
                            accountAddress = accountAddress,
                            isNewlyReceived = false,
                            addedAt = System.currentTimeMillis(),
                            tokenMetadata = selectedToken.metadata,
                            isHidden = selectedToken.isHidden,
                            isInDenyList = selectedToken.isInDenyList,
                            isInAllowList =
                            if (selectedToken.tokenState?.moduleState?.allowList == true)
                                false
                            else
                                null
                        )
                    )
                }
            }
        // Hide each loaded not selected token.
        loadedTokens
            .filterNot(NewToken::isSelected)
            .filterIsInstance<PLTToken>()
            .mapTo(mutableSetOf(), PLTToken::tokenId)
            .forEach { loadedNotSelectedTokenId ->
                pltRepository.hideToken(
                    accountAddress = accountAddress,
                    tokenId = loadedNotSelectedTokenId,
                )
            }
    }

    private suspend fun updateCIS2Tokens(
        accountAddress: String,
        loadedTokens: List<NewToken>
    ) {
        loadedTokens
            .filter(NewToken::isSelected)
            .filterIsInstance<NewContractToken>()
            .forEach { selectedToken ->
                val existingContractToken =
                    contractTokensRepository.find(
                        accountAddress,
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
                            accountAddress = accountAddress,
                            isFungible = selectedToken.metadata?.unique?.not() ?: false,
                            tokenMetadata = selectedToken.metadata,
                            isNewlyReceived = false,
                            addedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

        // Delete each loaded not selected token.
        loadedTokens
            .filterNot(NewToken::isSelected)
            .filterIsInstance<NewContractToken>()
            .mapTo(mutableSetOf(), NewContractToken::token)
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
    fun lastTokenId() = tokens.filterIsInstance<NewContractToken>().last().uid
}
