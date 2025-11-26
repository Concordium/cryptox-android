package com.concordium.wallet.data.backend.tokenmetadata

import com.concordium.wallet.App
import com.concordium.wallet.data.model.ContractTokenMetadata
import com.concordium.wallet.data.model.ProtocolLevelTokenMetadata
import com.concordium.wallet.util.Log
import com.reown.util.bytesToHex
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
                    .header("User-Agent", "Mozilla/5.0 (Android)") // required for correct response from ipsf.io URLs
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

    private suspend fun <MetadataType> getMetadata(
        url: String,
        sha256HashHex: String?,
        metadataClass: Class<MetadataType>,
    ): Result<MetadataType> = runCatching {

        val responseJson = backend.getMetadataJson(url)

        if (sha256HashHex != null) {
            val responseJsonHash = responseJson.sha256().bytesToHex()
            if (responseJsonHash.lowercase() != sha256HashHex.lowercase()) {
                throw TokenMetadataHashException()
            }
        }

        return@runCatching App.appCore.gson.fromJson(
            responseJson,
            metadataClass
        )
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
