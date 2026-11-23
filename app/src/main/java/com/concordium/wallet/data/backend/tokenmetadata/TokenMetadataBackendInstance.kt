package com.concordium.wallet.data.backend.tokenmetadata

import com.concordium.wallet.App
import com.concordium.wallet.data.model.ContractTokenMetadata
import com.concordium.wallet.data.model.ProtocolLevelTokenMetadata
import com.concordium.wallet.util.IpfsGatewayFallback
import com.concordium.wallet.util.Log
import com.google.gson.JsonParser
import com.reown.util.bytesToHex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.komputing.khash.sha256.extensions.sha256
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class TokenMetadataHashException :
    IOException("Actual metadata hash doesn't match the required one")

object TokenMetadataBackendInstance {

    private val backend by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cache(null)
            .addInterceptor(
                HttpLoggingInterceptor(Log::i)
                    .setLevel(HttpLoggingInterceptor.Level.BASIC)
            )
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://some.api.url/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(TokenMetadataBackend::class.java)
    }

    /**
     * Try the original URL first, then alternative IPFS gateways.
     */
    private suspend fun <MetadataType> fetchMetadata(
        originalUrl: String,
        sha256HashHex: String?,
        metadataClass: Class<MetadataType>,
    ): MetadataType {
        val urls = IpfsGatewayFallback.getUrls(originalUrl)
        var lastError: Exception? = null

        for ((index, url) in urls.withIndex()) {
            try {
                val responseJson = if (urls.size > 1) {
                    // Apply a shorter timeout only when IPFS fallbacks are available.
                    withTimeout(8000L) {
                        backend.getMetadataJson(url)
                    }
                } else {
                    // Preserve the existing OkHttp timeouts for regular URLs.
                    backend.getMetadataJson(url)
                }

                // Preserve the existing SHA-256 verification.
                if (sha256HashHex != null) {
                    val actualHash = responseJson.sha256().bytesToHex()

                    if (!actualHash.equals(
                            sha256HashHex,
                            ignoreCase = true
                        )
                    ) {
                        throw TokenMetadataHashException()
                    }
                }

                // Ensure the response is a JSON object.
                val jsonElement = JsonParser.parseString(responseJson)

                if (!jsonElement.isJsonObject) {
                    throw IOException("Invalid token metadata JSON")
                }

                val metadata = App.appCore.gson.fromJson(
                    responseJson,
                    metadataClass
                ) ?: throw IOException("Empty token metadata")

                Log.i(
                    "Token metadata loaded from " +
                        if (index == 0) {
                            "original URL"
                        } else {
                            "IPFS fallback: $url"
                        }
                )

                return metadata
            } catch (e: TimeoutCancellationException) {
                lastError = IOException(
                    "IPFS gateway timed out after 8000 ms: $url",
                    e
                )

                Log.w(
                    "Token metadata timeout, trying next gateway: $url"
                )

            } catch (e: CancellationException) {
                // Preserve cancellation of the parent coroutine.
                throw e

            } catch (e: TokenMetadataHashException) {
                // Do not bypass a metadata hash mismatch.
                throw e

            } catch (e: Exception) {
                lastError = e

                Log.w(
                    "Token metadata request failed: " +
                        "$url (${e.message})"
                )
            }
        }

        throw lastError ?: IOException(
            "Failed to load token metadata"
        )
    }

    private suspend fun <MetadataType> getMetadata(
        url: String,
        sha256HashHex: String?,
        metadataClass: Class<MetadataType>,
    ): Result<MetadataType> {
        return try {
            Result.success(
                fetchMetadata(
                    originalUrl = url,
                    sha256HashHex = sha256HashHex,
                    metadataClass = metadataClass,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContractTokenMetadata(
        url: String,
        sha256HashHex: String?,
    ): Result<ContractTokenMetadata> =
        getMetadata(
            url = url,
            sha256HashHex = sha256HashHex,
            metadataClass = ContractTokenMetadata::class.java,
        )

    suspend fun getProtocolLevelTokenMetadata(
        url: String,
        sha256HashHex: String?,
    ): Result<ProtocolLevelTokenMetadata> =
        getMetadata(
            url = url,
            sha256HashHex = sha256HashHex,
            metadataClass = ProtocolLevelTokenMetadata::class.java,
        )
}
