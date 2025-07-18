package com.concordium.wallet.core.tokens

import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.PLTRepository
import com.concordium.wallet.data.backend.price.TokenPriceRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.data.model.PLTToken
import com.concordium.wallet.data.model.toNewContractToken
import com.concordium.wallet.data.model.toNewPLTToken

class TokensInteractor(
    private val proxyRepository: ProxyRepository,
    private val contractTokensRepository: ContractTokensRepository,
    private val pltRepository: PLTRepository,
    private val accountRepository: AccountRepository,
    private val tokenPriceRepository: TokenPriceRepository,
    private val loadTokensBalancesUseCase: LoadTokensBalancesUseCase
) {

    suspend fun loadTokens(accountAddress: String, loadBalances: Boolean = true): Result<List<NewToken>> {
        return try {
            val ccdToken = getCCDDefaultToken(accountAddress)

            val contractTokens = contractTokensRepository.getTokens(
                accountAddress = accountAddress,
                isFungible = null,
            ).map { it.toNewContractToken() }

            //TODO: remove hardcoded PLT address
            val pltTokens =
                pltRepository.getTokens("4GbHu8Ynnt1hc2PGhRAiwGzkXYBxnSCNJEB9dcnGEJPehRw3oo")
                    .filterNot { it.isHidden }
                    .map { it.toNewPLTToken() }

            val allTokens = (listOf(ccdToken) + contractTokens + pltTokens).sortedBy { it.addedAt }

            if (loadBalances) {
                loadTokensBalancesUseCase(
                    proxyRepository = proxyRepository,
                    tokens = allTokens,
                    accountAddress = accountAddress,
                )
            }

            Result.success(allTokens)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
                //TODO: remove hardcoded PLT address
                pltRepository.hideToken(
                    "4GbHu8Ynnt1hc2PGhRAiwGzkXYBxnSCNJEB9dcnGEJPehRw3oo",
                    token.tokenId
                )
                Result.success(true)
            }

            else -> Result.failure(UnsupportedOperationException("Cannot delete CCD token"))
        }
    }

    private suspend fun getCCDDefaultToken(accountAddress: String): CCDToken {
        val account = accountRepository.findByAddress(accountAddress)
            ?: error("Account $accountAddress not found")
        val eurPerMicroCcd = tokenPriceRepository.getEurPerMicroCcd()
            .getOrNull()

        return CCDToken(
            balance = account.balance,
            accountAddress = accountAddress,
            isNewlyReceived = false,
            isSelected = false,
            eurPerMicroCcd = eurPerMicroCcd,
            isEarning = account.isBaking() || account.isDelegating(),
        )
    }
}