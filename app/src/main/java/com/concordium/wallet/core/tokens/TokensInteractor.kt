package com.concordium.wallet.core.tokens

import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.PLTRepository
import com.concordium.wallet.data.backend.price.TokenPriceRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.PLTInfo
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.data.model.toNewContractToken
import com.concordium.wallet.data.model.toPLTToken

class TokensInteractor(
    private val proxyRepository: ProxyRepository,
    private val contractTokensRepository: ContractTokensRepository,
    private val pltRepository: PLTRepository,
    private val accountRepository: AccountRepository,
    private val tokenPriceRepository: TokenPriceRepository,
    private val loadTokensBalancesUseCase: LoadTokensBalancesUseCase,
    private val loadCIS2TokensUseCase: LoadCIS2TokensUseCase,
    private val loadCIS2TokensMetadataUseCase: LoadCIS2TokensMetadataUseCase
) {

    suspend fun loadTokens(
        accountAddress: String,
        loadBalances: Boolean = true,
        onlyTransferable: Boolean = false,
        addCCDToken: Boolean = true,
    ): Result<List<NewToken>> {
        return try {
            val ccdToken = getCCDDefaultToken(accountAddress)

            val contractTokens = contractTokensRepository.getTokens(
                accountAddress = accountAddress,
                isFungible = null,
            ).map { it.toNewContractToken() }

            val pltTokens =
                pltRepository.getTokens(accountAddress)
                    .filterNot { it.isHidden }
                    .map { it.toPLTToken() }
                    .filter { !onlyTransferable || it.isTransferable }

            val allTokens = buildList {
                if (addCCDToken) add(ccdToken)
                addAll(contractTokens)
                addAll(pltTokens)
            }

            if (loadBalances) {
                loadTokensBalances(
                    accountAddress = accountAddress,
                    tokens = allTokens,
                )
            }

            Result.success(allTokens)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadTokensBalances(
        accountAddress: String,
        tokens: List<NewToken>,
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

    suspend fun loadCIS2TokensMetadata(tokens: List<NewContractToken>) {
        loadCIS2TokensMetadataUseCase(
            proxyRepository = proxyRepository,
            tokensToUpdate = tokens
        )
    }

    suspend fun getPLTTokenById(tokenId: String): Result<PLTInfo> = runCatching {
        proxyRepository.getPLTTokenById(tokenId)
    }

    suspend fun deleteToken(
        accountAddress: String,
        token: NewToken,
    ): Result<Boolean> {
        return when (token) {
            is NewContractToken -> {
                contractTokensRepository.delete(
                    accountAddress = accountAddress,
                    contractIndex = token.contractIndex,
                    token = token.token,
                )
                Result.success(true)
            }

            is PLTToken -> {
                pltRepository.hideToken(accountAddress, token.tokenId)
                Result.success(true)
            }

            else -> Result.failure(UnsupportedOperationException("Cannot delete CCD token"))
        }
    }

    suspend fun unmarkNewlyReceivedToken(token: NewToken) {
        when (token) {
            is NewContractToken -> contractTokensRepository.unmarkNewlyReceived(token.token)
            is PLTToken -> pltRepository.unmarkNewlyReceived(token.tokenId)
            else -> throw UnsupportedOperationException("Cannot unmark CCD token")
        }
    }

    private suspend fun getCCDDefaultToken(
        accountAddress: String,
    ) = CCDToken(
        account = accountRepository.findByAddress(accountAddress)
            ?: error("Account $accountAddress not found"),
        eurPerMicroCcd = tokenPriceRepository.getEurPerMicroCcd()
            .getOrNull()
    )
}
