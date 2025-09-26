package com.concordium.wallet.core.tokens

import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.PLTRepository
import com.concordium.wallet.data.backend.price.TokenPriceRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.backend.tokenmetadata.TokenMetadataBackendInstance
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.ProtocolLevelToken
import com.concordium.wallet.data.model.ProtocolLevelTokenMetadata
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.toContractToken
import com.concordium.wallet.data.room.ContractTokenEntity
import com.concordium.wallet.data.room.ProtocolLevelTokenEntity
import com.concordium.wallet.util.Log

class TokensInteractor(
    private val proxyRepository: ProxyRepository,
    private val tokenPriceRepository: TokenPriceRepository,
    private val loadTokensBalancesUseCase: LoadTokensBalancesUseCase,
    private val loadCIS2TokensUseCase: LoadCIS2TokensUseCase,
    private val loadCIS2TokensMetadataUseCase: LoadCIS2TokensMetadataUseCase,
) {

    suspend fun loadTokens(
        accountAddress: String,
        loadBalances: Boolean = true,
        onlyTransferable: Boolean = false,
        addCCDToken: Boolean = true,
        ccdWithTotalBalance: Boolean = false,
    ): Result<List<Token>> = runCatching {

        val contractTokensRepository = ContractTokensRepository(
            App.appCore.session.walletStorage.database.contractTokenDao()
        )

        val ccdToken = getCCDDefaultToken(accountAddress, ccdWithTotalBalance)

        val contractTokens =
            contractTokensRepository
                .getTokens(accountAddress)
                .map(ContractTokenEntity::toContractToken)

        val pltRepository = PLTRepository(
            App.appCore.session.walletStorage.database.protocolLevelTokenDao()
        )

        val pltTokens =
            pltRepository
                .getTokens(accountAddress)
                .filterNot(ProtocolLevelTokenEntity::isHidden)
                .map { it.toProtocolLevelToken() }
                .filter { !onlyTransferable || it.isTransferable }

        val allTokens = buildList {
            if (addCCDToken) add(ccdToken)
            addAll((contractTokens + pltTokens).sortedByDescending { it.addedAt })
        }

        if (loadBalances) {
            loadTokensBalances(
                accountAddress = accountAddress,
                tokens = allTokens,
            )
        }
        allTokens
    }

    suspend fun loadTokensBalances(
        accountAddress: String,
        tokens: List<Token>,
    ) = runCatching {
        loadTokensBalancesUseCase(
            proxyRepository = proxyRepository,
            tokens = tokens,
            accountAddress = accountAddress,
        )
    }

    suspend fun loadCIS2TokensWithBalance(
        accountAddress: String,
        contractIndex: String,
        subIndex: String,
        limit: Int,
        from: String? = null,
    ) = runCatching {
        val tokens = loadCIS2TokensUseCase(
            proxyRepository = proxyRepository,
            contractIndex = contractIndex,
            subIndex = subIndex,
            limit = limit,
            from = from,
        )
        loadTokensBalances(
            accountAddress = accountAddress,
            tokens = tokens

        )
        tokens
    }

    suspend fun loadCIS2TokensMetadata(tokens: List<ContractToken>) {
        loadCIS2TokensMetadataUseCase(
            proxyRepository = proxyRepository,
            tokensToUpdate = tokens
        )
    }

    suspend fun loadProtocolLevelTokenWithMetadata(
        accountAddress: String,
        tokenId: String,
    ): Result<ProtocolLevelToken> = runCatching {

        val tokenInfo = proxyRepository.getPLTTokenById(tokenId)
        val metadataInfo = tokenInfo.tokenState.moduleState.metadata

        val verifiedMetadata: ProtocolLevelTokenMetadata? =
            if (metadataInfo != null)
                TokenMetadataBackendInstance
                    .getProtocolLevelTokenMetadata(
                        url = metadataInfo.url,
                        sha256HashHex = metadataInfo.checksumSha256,
                    )
                    .onFailure {
                        Log.w(
                            "Failed loading metadata for token ${tokenInfo.tokenId}: " +
                                    it.message
                        )
                    }
                    .getOrNull()
            else
                null

        return@runCatching tokenInfo.toProtocolLevelToken(
            accountAddress = accountAddress,
            metadata = verifiedMetadata,
        )
    }

    suspend fun deleteToken(
        accountAddress: String,
        token: Token,
    ): Result<Boolean> {
        return when (token) {
            is ContractToken -> {
                val contractTokensRepository = ContractTokensRepository(
                    App.appCore.session.walletStorage.database.contractTokenDao()
                )
                contractTokensRepository.delete(
                    accountAddress = accountAddress,
                    contractIndex = token.contractIndex,
                    token = token.token,
                )
                Result.success(true)
            }

            is ProtocolLevelToken -> {
                val pltRepository = PLTRepository(
                    App.appCore.session.walletStorage.database.protocolLevelTokenDao()
                )
                pltRepository.hideToken(accountAddress, token.tokenId)
                Result.success(true)
            }

            else -> Result.failure(UnsupportedOperationException("Cannot delete CCD token"))
        }
    }

    suspend fun unmarkNewlyReceivedToken(token: Token) {
        when (token) {
            is ContractToken -> {
                val contractTokensRepository = ContractTokensRepository(
                    App.appCore.session.walletStorage.database.contractTokenDao()
                )
                contractTokensRepository.unmarkNewlyReceived(token.token)
            }

            is ProtocolLevelToken -> {
                val pltRepository = PLTRepository(
                    App.appCore.session.walletStorage.database.protocolLevelTokenDao()
                )
                pltRepository.unmarkNewlyReceived(token.tokenId)
            }

            else -> throw UnsupportedOperationException("Cannot unmark CCD token")
        }
    }

    private suspend fun getCCDDefaultToken(
        accountAddress: String,
        withTotalBalance: Boolean = false,
    ): CCDToken {
        val accountRepository = AccountRepository(
            App.appCore.session.walletStorage.database.accountDao()
        )
        return CCDToken(
            account = accountRepository.findByAddress(accountAddress)
                ?: error("Account $accountAddress not found"),
            withTotalBalance = withTotalBalance,
            eurPerMicroCcd = tokenPriceRepository.getEurPerMicroCcd()
                .getOrNull()
        )
    }
}
