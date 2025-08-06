package com.concordium.wallet.data.backend.notifications

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

class NotificationsBackendConfig(val gson: Gson) {

    val retrofit: Retrofit
    val backend: NotificationsBackend

    init {
        retrofit = initializeRetrofit(BuildConfig.URL_NOTIFICATIONS_BASE)
        backend = retrofit.create(NotificationsBackend::class.java)
    }

    private fun initializeRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
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
