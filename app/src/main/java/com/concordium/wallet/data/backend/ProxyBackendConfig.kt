package com.concordium.wallet.data.backend

import com.concordium.wallet.AppConfig
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor

class ProxyBackendConfig(val gson: Gson) {

    val retrofit: Retrofit
    val backend: ProxyBackend

    init {
        retrofit = initializeRetrofit(AppConfig.proxyBaseUrl)
        backend = retrofit.create(ProxyBackend::class.java)
    }

    private fun initializeRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(initializeOkkHttp())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun initializeOkkHttp(): OkHttpClient {
        var okHttpClientBuilder = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cache(null)
            .addInterceptor(ModifyHeaderInterceptor())
            .addInterceptor(
                LoggingInterceptor.Builder()
                    .setLevel(Level.BASIC)
                    .log(Platform.INFO)
//                    .logger(LoggerX())
                    .request("Request")
                    .response("Response")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
//                    .addHeader("Authorization", t)
                    .build()
            )

        if (AppConfig.useOfflineMock) {
            okHttpClientBuilder = okHttpClientBuilder.addInterceptor(OfflineMockInterceptor())
        }
        return okHttpClientBuilder.build()
    }
}