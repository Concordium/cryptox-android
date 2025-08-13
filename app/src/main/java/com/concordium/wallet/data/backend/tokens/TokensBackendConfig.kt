package com.concordium.wallet.data.backend.tokens

import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.backend.ModifyHeaderInterceptor
import com.google.gson.Gson
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// https://gitlab.com/tacans/spaceseven/services/protocol/proto/-/blob/master/api/v2/nft/api_get_by_address.proto

class TokensBackendConfig(private val gson: Gson) {

    val retrofit: Retrofit
    val backend: TokensBackend

    init {
        retrofit = initializeRetrofit()
        backend = retrofit.create(TokensBackend::class.java)
    }

    private fun initializeRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.S7_DOMAIN)
            .client(initializeOkkHttp())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun initializeOkkHttp(): OkHttpClient {
        return OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cache(null)
            .addInterceptor(ModifyHeaderInterceptor())
            .addInterceptor(
                LoggingInterceptor.Builder()
                    .setLevel(Level.BASIC)
                    .log(Platform.INFO)
                    .request("Request")
                    .response("Response")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
            )
            .build()
    }
}
