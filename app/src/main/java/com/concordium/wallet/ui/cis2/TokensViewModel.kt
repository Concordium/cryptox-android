package com.concordium.wallet.ui.cis2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.CIS2TokensMetadataItem
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TokenMetadata
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.AccountContract
import com.concordium.wallet.data.room.ContractToken
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.cis2.retrofit.MetadataApiInstance
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger
import com.walletconnect.util.Empty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable
import java.math.BigInteger

data class TokenData(
    var account: Account? = null,
    var selectedToken: Token? = null,
    var contractIndex: String = "",
    var subIndex: String = "0",
    var contractName: String = ""
) : Serializable

class TokensViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TOKENS_NOT_LOADED = -1
        const val TOKENS_OK = 0
        const val TOKENS_EMPTY = 1
        const val TOKENS_ERROR = 2
    }

    private var allowToLoadMore = true

    var tokenData = TokenData()
    var tokens: MutableList<Token> = mutableListOf()
    var searchedTokens: MutableList<Token> = mutableListOf()

    val chooseToken: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val chooseTokenInfo: MutableLiveData<Token> by lazy { MutableLiveData<Token>() }
    val waiting: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    private val errorInt: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val lookForTokens: MutableLiveData<Int> by lazy { MutableLiveData<Int>(TOKENS_NOT_LOADED) }
    val updateWithSelectedTokensDone: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val stepPageBy: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val tokenDetails: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val hasExistingAccountContract: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val nonSelected: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val tokenBalances: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    private val proxyRepository = ProxyRepository()

    /**
     * @param isFungible if set, allows loading only fungible or non-fungible
     */
    fun loadTokens(
        accountAddress: String,
        isFungible: Boolean? = null,
    ) {
        waiting.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val accountContractRepository = AccountContractRepository(
                WalletDatabase.getDatabase(getApplication()).accountContractDao()
            )
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
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

    fun lookForTokens(accountAddress: String, from: String? = null) {
        if (!allowToLoadMore)
            return

        allowToLoadMore = false

        if (from == null) {
            tokens.clear()
            lookForTokens.postValue(TOKENS_NOT_LOADED)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
            val existingContractTokens =
                contractTokensRepository.getTokens(accountAddress, tokenData.contractIndex)
            val existingTokens = existingContractTokens.map { it.tokenId }.toSet()
            proxyRepository.getCIS2Tokens(
                tokenData.contractIndex,
                tokenData.subIndex,
                from,
                success = { cis2Tokens ->
                    cis2Tokens.tokens.forEach { token ->
                        if (existingTokens.contains(token.token)) {
                            token.isSelected = true
                        }

                        token.contractIndex = tokenData.contractIndex
                        token.subIndex = tokenData.subIndex
                    }

                    // Do not add burnt tokens (total supply 0).
                    val filteredCis2Tokens = cis2Tokens.tokens
                        .filter { it.totalSupply != "0" }

                    tokens.addAll(filteredCis2Tokens)
                    if (filteredCis2Tokens.isEmpty()) {
                        lookForTokens.postValue(TOKENS_EMPTY)
                    } else {
                        loadTokensMetadata()
                        lookForTokens.postValue(TOKENS_OK)
                    }
                    allowToLoadMore = true
                },
                failure = {
                    lookForTokens.postValue(TOKENS_ERROR)
                    handleBackendError(it)
                    allowToLoadMore = true
                })
        }
    }

    fun toggleNewToken(token: Token) {
        tokens.firstOrNull { it.id == token.id }?.let {
            it.isSelected = it.isSelected == false
        }
    }

    fun hasExistingTokens() {
        val accountContractRepository = AccountContractRepository(
            WalletDatabase.getDatabase(getApplication()).accountContractDao()
        )
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
        if (searchedTokens.isNotEmpty()) {
            updateSearchedTokens(searchedTokens)
        } else {
            updateTokens(tokens)
        }
    }

    private fun updateSearchedTokens(updatedTokens: MutableList<Token>) {
        tokenData.account?.let { account ->
            val accountContractRepository = AccountContractRepository(
                WalletDatabase.getDatabase(getApplication()).accountContractDao()
            )
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
            CoroutineScope(Dispatchers.IO).launch {
                var anyChanges = false

                val accountContract =
                    accountContractRepository.find(account.address, tokenData.contractIndex)
                if (accountContract == null) {
                    accountContractRepository.insert(
                        AccountContract(
                            0,
                            account.address,
                            tokenData.contractIndex
                        )
                    )
                    anyChanges = true
                }

                updatedTokens.forEach { selectedToken ->

                    val existingContractToken =
                        contractTokensRepository.find(
                            account.address,
                            tokenData.contractIndex,
                            selectedToken.token
                        )

                    if (existingContractToken == null && selectedToken.isSelected) {
                        contractTokensRepository.insert(
                            ContractToken(
                                0,
                                tokenData.contractIndex,
                                selectedToken.token,
                                account.address,
                                !(selectedToken.tokenMetadata?.unique ?: false),
                                selectedToken.tokenMetadata,
                                tokenData.contractName
                            )
                        )
                        anyChanges = true
                    }
                    if (existingContractToken != null && !selectedToken.isSelected) {
                        deleteSingleToken(
                            account.address, tokenData.contractIndex,
                            selectedToken.token
                        )
                        anyChanges = true
                    }
                }
                updateWithSelectedTokensDone.postValue(anyChanges)
            }
        }
    }

    private fun updateTokens(updatedTokens: MutableList<Token>) {
        val selectedTokens = updatedTokens.filter(Token::isSelected)
        tokenData.account?.let { account ->
            val accountContractRepository = AccountContractRepository(
                WalletDatabase.getDatabase(getApplication()).accountContractDao()
            )
            val contractTokensRepository = ContractTokensRepository(
                WalletDatabase.getDatabase(getApplication()).contractTokenDao()
            )
            CoroutineScope(Dispatchers.IO).launch {
                var anyChanges = false
                if (selectedTokens.isEmpty()) {
                    val existingContractTokens =
                        contractTokensRepository.getTokens(account.address, tokenData.contractIndex)
                    existingContractTokens.forEach { existingContractToken ->
                        contractTokensRepository.delete(existingContractToken)
                        anyChanges = true
                    }
                    val existingAccountContract =
                        accountContractRepository.find(account.address, tokenData.contractIndex)
                    if (existingAccountContract == null) {
                        nonSelected.postValue(true)
                    } else {
                        accountContractRepository.delete(existingAccountContract)
                        anyChanges = true
                    }
                    updateWithSelectedTokensDone.postValue(anyChanges)
                } else {
                    val accountContract =
                        accountContractRepository.find(account.address, tokenData.contractIndex)
                    if (accountContract == null) {
                        accountContractRepository.insert(
                            AccountContract(
                                0,
                                account.address,
                                tokenData.contractIndex
                            )
                        )
                        anyChanges = true
                    }
                    val existingContractTokens =
                        contractTokensRepository.getTokens(account.address, tokenData.contractIndex)
                    val existingNotSelectedTokenIds = existingContractTokens.map { it.tokenId }
                        .minus(selectedTokens.map { it.token }.toSet())
                    existingNotSelectedTokenIds.forEach { existingNotSelectedTokenId ->
                        contractTokensRepository.find(
                            account.address,
                            tokenData.contractIndex,
                            existingNotSelectedTokenId
                        )?.let { existingNotSelectedContractToken ->
                            contractTokensRepository.delete(existingNotSelectedContractToken)
                            anyChanges = true
                        }
                    }
                    selectedTokens.forEach { selectedToken ->
                        val existingContractToken =
                            contractTokensRepository.find(
                                account.address,
                                tokenData.contractIndex,
                                selectedToken.token
                            )
                        if (existingContractToken == null) {
                            contractTokensRepository.insert(
                                ContractToken(
                                    0,
                                    tokenData.contractIndex,
                                    selectedToken.token,
                                    account.address,
                                    !(selectedToken.tokenMetadata?.unique ?: false),
                                    selectedToken.tokenMetadata,
                                    tokenData.contractName
                                )
                            )
                            anyChanges = true
                        }
                    }
                    updateWithSelectedTokensDone.postValue(anyChanges)
                }
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

    private fun loadTokensMetadata() = viewModelScope.launch(Dispatchers.IO) {
        val commaSeparated = tokens.filter { !it.isCCDToken }.joinToString(",") { it.token }
        proxyRepository.getCIS2TokenMetadata(
            tokenData.contractIndex,
            tokenData.subIndex,
            tokenIds = commaSeparated,
            success = { cis2TokensMetadata ->
                tokenData.contractName = cis2TokensMetadata.contractName
                tokens.forEach {
                    it.contractName = cis2TokensMetadata.contractName
                }

                cis2TokensMetadata.metadata.forEach { metadataItem ->
                    loadTokenMetadata(
                        tokenToUpdate = tokens.first {
                            it.token == metadataItem.tokenId
                                    && it.contractIndex == tokenData.contractIndex
                        },
                        cis2TokensMetadataItem = metadataItem,
                    )
                }
            },
            failure = {
                handleBackendError(it)
            })
    }

    private fun loadTokenMetadata(
        tokenToUpdate: Token,
        cis2TokensMetadataItem: CIS2TokensMetadataItem,
    ) {
        println("LC -> ${cis2TokensMetadataItem.metadataURL}")
        if (cis2TokensMetadataItem.metadataURL.isBlank())
            return
        viewModelScope.launch {
            val tokenMetadata =
                MetadataApiInstance.safeMetadataCall(cis2TokensMetadataItem.metadataURL)

            if (tokenMetadata != null) {
                tokenToUpdate.tokenMetadata = tokenMetadata
            } else {
                tokenToUpdate.tokenMetadata = TokenMetadata(
                    decimals = 0,
                    description = "Metadata is missing or malformed",
                    name = "Token ${tokenToUpdate.token}@${tokenToUpdate.contractIndex}",
                    symbol = "",
                    thumbnail = null,
                    unique = false,
                    display = null,
                )
            }
            tokenDetails.postValue(true)
        }
    }

    fun loadTokensBalances() = viewModelScope.launch {
        if (tokenData.account == null)
            return@launch

        // Update CCD balance separately.
        tokens.find { it.isCCDToken }?.also { ccdToken ->
            val updatedCcdToken = getCCDDefaultToken(tokenData.account!!.address)
            ccdToken.totalBalance = updatedCcdToken.totalBalance
            ccdToken.atDisposal = updatedCcdToken.atDisposal
        }

        // Load non-CCD balances grouping the tokens by contract.
        tokens.filter { !it.isCCDToken }.groupBy { it.contractIndex }.forEach { group ->
            val groupTokens = group.value
            val commaSeparated = groupTokens.joinToString(",") { it.token }
            proxyRepository.getCIS2TokenBalance(
                group.key,
                tokenData.subIndex,
                tokenData.account!!.address,
                commaSeparated,
                success = { cis2TokensBalances ->
                    cis2TokensBalances.forEach { cis2TokenBalance ->
                        groupTokens.firstOrNull { it.token == cis2TokenBalance.tokenId }?.totalBalance =
                            cis2TokenBalance.balance.toBigInteger()
                    }
                    tokenBalances.postValue(true)
                },
                failure = {
                    handleBackendError(it)
                }
            )
        }
    }

    fun deleteSingleToken(accountAddress: String, contractIndex: String, tokenId: String) {
        val contractTokensRepository = ContractTokensRepository(
            WalletDatabase.getDatabase(getApplication()).contractTokenDao()
        )
        val accountContractRepository = AccountContractRepository(
            WalletDatabase.getDatabase(getApplication()).accountContractDao()
        )

        CoroutineScope(Dispatchers.IO).launch {

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
    }

    fun onFindTokensDialogDismissed() {
        resetLookForTokens()
    }

    private fun resetLookForTokens() {
        tokenData.contractIndex = String.Empty
        stepPageBy.value = 0
        lookForTokens.value = TOKENS_NOT_LOADED
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        errorInt.postValue(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
