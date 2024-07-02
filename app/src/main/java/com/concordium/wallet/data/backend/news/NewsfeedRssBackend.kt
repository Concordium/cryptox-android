package com.concordium.wallet.data.backend.news

import com.concordium.wallet.data.model.RssChannel
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NewsfeedRssBackend(
    private val articlesFeedUrl: String,
    private val okHttpClient: OkHttpClient,
    private val xmlMapper: XmlMapper,
) {
    suspend fun getArticlesItems(): List<RssChannel.Item> =
        getChannel(articlesFeedUrl).items

    private suspend fun getChannel(
        feedUrl: String
    ): RssChannel = suspendCancellableCoroutine { continuation ->
        val call = okHttpClient.newCall(
            Request.Builder()
                .url(feedUrl)
                .build()
        )
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    check(response.isSuccessful) {
                        "Request failed: ${response.code}"
                    }

                    val feedResponse = xmlMapper.readValue(
                        response.body?.byteStream(),
                        RssFeedResponse::class.java
                    )

                    continuation.resume(feedResponse.channel)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }
}
