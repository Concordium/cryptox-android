package com.concordium.wallet.core.tokens

import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.NewContractToken
import com.concordium.wallet.data.model.NewToken
import com.concordium.wallet.util.Log
import com.concordium.wallet.util.toBigInteger

class LoadTokensBalancesUseCase {

    suspend operator fun invoke(
        proxyRepository: ProxyRepository,
        tokens: List<NewToken>,
        accountAddress: String,
    ) {
        val tokensByContract: Map<String, List<NewContractToken>> = tokens
            .filterIsInstance<NewContractToken>()
            .groupBy { it.contractIndex }

        tokensByContract.forEach { (contractIndex, contractTokens) ->
            val contractSubIndex = contractTokens.firstOrNull()?.subIndex
                ?: return@forEach

            contractTokens
                .chunked(ProxyRepository.CIS_2_TOKEN_BALANCE_MAX_TOKEN_IDS)
                .forEach { contractTokensChunk ->
                    val commaSeparatedChunkTokenIds = contractTokensChunk.joinToString(
                        separator = ",",
                        transform = NewContractToken::token,
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
}