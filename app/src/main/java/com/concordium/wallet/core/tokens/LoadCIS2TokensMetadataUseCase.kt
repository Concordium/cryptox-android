package com.concordium.wallet.core.tokens

import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.backend.tokenmetadata.TokenMetadataBackendInstance
import com.concordium.wallet.data.backend.tokenmetadata.TokenMetadataHashException
import com.concordium.wallet.data.model.ContractToken
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive

class LoadCIS2TokensMetadataUseCase {

    suspend operator fun invoke(
        proxyRepository: ProxyRepository,
        tokensToUpdate: List<ContractToken>,
    ) = runCatching {
        coroutineScope {
            val tokensByContract: Map<String, List<ContractToken>> = tokensToUpdate
                .groupBy(ContractToken::contractIndex)

            tokensByContract.forEach { (contractIndex, contractTokens) ->
                val contractSubIndex = contractTokens.firstOrNull()?.subIndex
                    ?: return@forEach

                contractTokens
                    .chunked(ProxyRepository.CIS_2_TOKEN_METADATA_MAX_TOKEN_IDS)
                    .forEach { contractTokensChunk ->
                        val commaSeparatedChunkTokenIds = contractTokensChunk.joinToString(
                            separator = ",",
                            transform = ContractToken::token,
                        )

                        val ciS2TokensMetadata = proxyRepository.getCIS2TokenMetadataV1(
                            index = contractIndex,
                            subIndex = contractSubIndex,
                            tokenIds = commaSeparatedChunkTokenIds,
                        )
                        ciS2TokensMetadata.metadata
                            .filterNot { it.metadataURL.isBlank() }
                            // Request the actual metadata in parallel.
                            .map { metadataItem ->
                                async(Dispatchers.IO) {
                                    try {
                                        val verifiedMetadata = TokenMetadataBackendInstance
                                            .getContractTokenMetadata(
                                                url = metadataItem.metadataURL,
                                                sha256HashHex = metadataItem.metadataChecksum,
                                            ).getOrThrow()
                                        val correspondingToken = contractTokens.first {
                                            it.token == metadataItem.tokenId
                                        }

                                        correspondingToken.metadata = verifiedMetadata
                                        correspondingToken.contractName =
                                            ciS2TokensMetadata.contractName
                                    } catch (e: TokenMetadataHashException) {
                                        Log.w(
                                            "Metadata hash mismatch:\n" +
                                                    "metadataItem=$metadataItem"
                                        )
                                    } catch (e: Throwable) {
                                        ensureActive()
                                        Log.e(
                                            "Failed to load metadata:\n" +
                                                    "metadataItem=$metadataItem" +
                                                    e
                                        )
                                    }
                                }
                            }.awaitAll()
                    }
            }
        }
    }
}
