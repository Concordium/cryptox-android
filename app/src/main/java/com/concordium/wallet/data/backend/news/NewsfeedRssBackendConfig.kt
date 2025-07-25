package com.concordium.wallet.data.backend.news

import com.concordium.wallet.AppConfig
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.backend.ModifyHeaderInterceptor
import com.concordium.wallet.data.backend.OfflineMockInterceptor
import com.ctc.wstx.stax.WstxInputFactory
import com.ctc.wstx.stax.WstxOutputFactory
import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import java.util.concurrent.TimeUnit

class NewsfeedRssBackendConfig {

    val backend: NewsfeedRssBackend

    init {
        backend = NewsfeedRssBackend(
            articlesFeedUrl = BuildConfig.URL_NEWSFEED_ARTICLES_RSS,
            okHttpClient = initializeOkkHttp(),
            xmlMapper = initializeXmlMapper(),
        )
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
                    .request("Request")
                    .response("Response")
                    .addHeader("Accept", "application/rss+xml")
                    .build()
            )

        if (AppConfig.useOfflineMock) {
            okHttpClientBuilder = okHttpClientBuilder.addInterceptor(OfflineMockInterceptor())
        }

        return okHttpClientBuilder.build()
    }

    private fun initializeXmlMapper(): XmlMapper =
        XmlMapper.builder(
            XmlFactory.builder()
                .inputFactory(WstxInputFactory())
                .outputFactory(WstxOutputFactory())
                .build()
        ).build()
}
