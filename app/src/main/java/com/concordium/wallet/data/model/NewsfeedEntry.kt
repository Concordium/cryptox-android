package com.concordium.wallet.data.model

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

sealed interface NewsfeedEntry {
    val title: String
    val description: String?
    val thumbnailUrl: String?
    val url: String
    val date: Date

    data class Article(
        override val title: String,
        override val description: String?,
        override val thumbnailUrl: String?,
        override val url: String,
        override val date: Date
    ) : NewsfeedEntry {
        constructor(rssChannelItem: RssChannel.Item) : this(
            title = rssChannelItem.title.trim(),
            description = rssChannelItem.description.trim().takeIf(String::isNotEmpty),
            thumbnailUrl = rssChannelItem.thumbnail?.url,
            url = rssChannelItem.link,
            date = LocalDateTime
                .parse(rssChannelItem.pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant(ZoneOffset.UTC)
                .let(Date::from)
        )
    }
}
