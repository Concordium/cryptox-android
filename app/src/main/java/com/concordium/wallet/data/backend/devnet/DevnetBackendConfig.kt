package com.concordium.wallet.data.backend.devnet

import com.concordium.wallet.AppConfig
import com.concordium.wallet.data.backend.InMemoryCookieJar
import com.concordium.wallet.data.backend.ModifyHeaderInterceptor
import com.concordium.wallet.data.backend.OfflineMockInterceptor
import com.google.gson.Gson
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class DevnetBackendConfig(val gson: Gson) {

    val retrofit: Retrofit by lazy { initializeRetrofit() }

    val backend: DevnetBackend by lazy { retrofit.create(DevnetBackend::class.java) }

    private fun initializeRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
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
            .cookieJar(InMemoryCookieJar())
            .addInterceptor(ModifyHeaderInterceptor())
            .addInterceptor(
                LoggingInterceptor.Builder()
                    .setLevel(Level.BASIC)
                    .log(Platform.INFO)
                    .request("Request")
                    .response("Response")
                    .addHeader("Accept", "*/*")
                    .build()
            )

        if (AppConfig.useOfflineMock) {
            okHttpClientBuilder = okHttpClientBuilder.addInterceptor(OfflineMockInterceptor())
        }
        return okHttpClientBuilder.build()
    }

    companion object {
        private const val BASE_URL = "https://wallet-proxy.devnet-plt-beta.concordium.com"
    }
}