package com.concordium.wallet.data.model

import java.util.Date

sealed interface NewsfeedEntry {
    val title: String
    val description: String?
    val thumbnailUrl: String
    val url: String
    val date: Date

    data class Article(
        override val title: String,
        override val description: String?,
        override val thumbnailUrl: String,
        override val url: String,
        override val date: Date
    ) : NewsfeedEntry
}
