package com.concordium.wallet.core.tokens

import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.toContractToken

class LoadCIS2TokensUseCase {
    private val loadCIS2TokensMetadataUseCase = LoadCIS2TokensMetadataUseCase()
    /**
     * @return page of tokens with fully loaded metadata and balances.
     * If its size is smaller than [limit], consider this page the last one.
     */
    suspend operator fun invoke(
        proxyRepository: ProxyRepository,
        contractIndex: String = "",
        subIndex: String = "0",
        limit: Int,
        from: String? = null,
    ): List<ContractToken> {
        val fullyLoadedTokens = mutableListOf<Token>()

        var tokenPageCursor = from
        var isLastTokenPage = false
        while (fullyLoadedTokens.size < limit && !isLastTokenPage) {
            val pageTokens = proxyRepository.getCIS2Tokens(
                index = contractIndex,
                subIndex = subIndex,
                limit = limit,
                from = tokenPageCursor,
            ).tokens
                .map { it.toContractToken() }
                .onEach {
                    it.contractIndex = contractIndex
                    it.subIndex = subIndex
                }

            isLastTokenPage = pageTokens.size < limit
            tokenPageCursor = pageTokens.lastOrNull()?.uid

            loadCIS2TokensMetadataUseCase(proxyRepository, pageTokens)
            fullyLoadedTokens.addAll(pageTokens)
        }

        return fullyLoadedTokens.map { it as ContractToken }
    }
}