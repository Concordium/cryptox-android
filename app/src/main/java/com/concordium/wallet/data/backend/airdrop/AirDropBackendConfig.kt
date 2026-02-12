package com.concordium.wallet.data.backend.airdrop

import com.concordium.wallet.data.backend.ModifyHeaderInterceptor
import com.google.gson.Gson
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AirDropBackendConfig(
    private val spacesevenUrl: HttpUrl,
    private val gson: Gson,
) {

    private val retrofit: Retrofit
    val backend: AirDropBackend

    init {
        retrofit = initializeRetrofit()
        backend = retrofit.create(AirDropBackend::class.java)
    }

    private fun initializeRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(spacesevenUrl)
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
