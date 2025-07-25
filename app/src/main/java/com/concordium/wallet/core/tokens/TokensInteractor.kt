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
) {

    suspend fun loadTokens(
        accountAddress: String,
        loadBalances: Boolean = true,
        onlyTransferable: Boolean = false,
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

            val allTokens = (listOf(ccdToken) + contractTokens + pltTokens).sortedBy { it.addedAt }

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
